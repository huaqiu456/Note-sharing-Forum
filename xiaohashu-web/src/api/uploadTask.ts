import {
  abortMultipartUpload,
  completeMultipartUpload,
  getMultipartStatus,
  getPartUrls,
  initMultipartUpload,
  presignDirectUpload
} from './file'

const MB = 1024 * 1024
const DIRECT_UPLOAD_LIMIT = 10 * MB
const MAX_FILE_SIZE = 100 * MB
const PART_SIZE = 10 * MB
const MAX_CONCURRENT_PARTS = 3
const MAX_RETRY = 3
const STORAGE_PREFIX = 'multipart-upload:'

export interface UploadProgress {
  loaded: number
  total: number
  percent: number
}

/**
 * 可控上传任务。
 *
 * <p>大文件支持暂停、继续、取消；小文件只支持进度和取消。
 * 小文件没有 uploadId，也没有服务端 Multipart 会话，因此不能暂停后继续。
 */
export interface UploadTask {
  readonly file: File
  start: () => Promise<string>
  pause: () => void
  resume: () => Promise<string>
  cancel: () => Promise<void>
  isPaused: () => boolean
  isCancelled: () => boolean
}

/**
 * 暂停上传时使用的内部异常。
 *
 * <p>它不是业务错误，只用于让并发分片任务快速退出，然后等待用户继续。
 */
export class UploadPausedError extends Error {
  constructor() {
    super('上传已暂停')
    this.name = 'UploadPausedError'
  }
}

/**
 * 取消上传时使用的异常。
 */
export class UploadCancelledError extends Error {
  constructor() {
    super('上传已取消')
    this.name = 'UploadCancelledError'
  }
}

export function isUploadCancelledError(error: unknown): error is UploadCancelledError {
  return error instanceof UploadCancelledError
}

/**
 * 创建前端可控上传任务。
 *
 * <p>文件大小规则：
 * <= 10MB 走单次预签名 PUT；
 * > 10MB 走 S3 Multipart；
 * > 100MB 直接拒绝。
 */
export function createUploadTask(
  file: File,
  onProgress?: (progress: UploadProgress) => void
): UploadTask {
  if (file.size <= 0) {
    throw new Error('文件不能为空')
  }
  if (file.size > MAX_FILE_SIZE) {
    throw new Error('文件不能超过 100MB')
  }

  const useMultipart = file.size > DIRECT_UPLOAD_LIMIT
  let started = false
  let promise: Promise<string> | null = null
  let paused = false
  let cancelled = false
  let resumeWaiters: Array<() => void> = []
  let currentAbortController: AbortController | null = null

  // Multipart 会话状态。小文件不会使用这些变量。
  let uploadId: string | null = null
  let storageKey: string | null = null
  let currentPartSize = PART_SIZE
  let uploadedParts = new Set<number>()
  let partProgress = new Map<number, number>()
  let needsStatusRefresh = false

  const notifyProgress = (loaded: number, total: number) => {
    onProgress?.({
      loaded,
      total,
      percent: total > 0 ? Math.min(100, Math.round((loaded / total) * 100)) : 0
    })
  }

  const waitForResume = () => new Promise<void>(resolve => {
    if (!paused || cancelled) {
      resolve()
      return
    }
    resumeWaiters.push(resolve)
  })

  const wakeResumeWaiters = () => {
    const waiters = resumeWaiters
    resumeWaiters = []
    waiters.forEach(resolve => resolve())
  }

  const partSizeOf = (partNumber: number) => {
    const start = (partNumber - 1) * currentPartSize
    return Math.min(currentPartSize, file.size - start)
  }

  const notifyLoaded = () => {
    const loaded = Array.from(partProgress.values())
      .reduce((sum, value) => sum + value, 0)
    notifyProgress(loaded, file.size)
  }

  const applyUploadedParts = (parts: number[], partSize: number) => {
    currentPartSize = partSize > 0 ? partSize : PART_SIZE
    uploadedParts = new Set(parts)
    partProgress = new Map()
    uploadedParts.forEach(partNumber => {
      partProgress.set(partNumber, partSizeOf(partNumber))
    })
    notifyLoaded()
  }

  const safeAbortMultipart = async (id: string) => {
    try {
      await abortMultipartUpload(id)
    } catch (error) {
      // 取消是最终用户动作，即使服务端会话已经不存在，也不应该让 UI 再次报错。
      console.warn('[oss-upload] 取消 Multipart 会话失败', id, error)
    }
  }

  const ensureMultipartSession = async () => {
    if (uploadId) {
      return
    }

    const fingerprint = await buildFingerprint(file)
    storageKey = `${STORAGE_PREFIX}${fingerprint}`
    const totalParts = Math.ceil(file.size / PART_SIZE)
    const saved = readUploadId(storageKey)

    if (saved) {
      try {
        const status = await getMultipartStatus(saved)
        if (cancelled) {
          await safeAbortMultipart(status.uploadId)
          localStorage.removeItem(storageKey)
          throw new UploadCancelledError()
        }
        uploadId = status.uploadId
        applyUploadedParts(status.uploadedParts || [], status.partSize)
        console.info('[oss-upload] 恢复 Multipart 上传', uploadId, status.uploadedParts)
        return
      } catch (error) {
        if (error instanceof UploadCancelledError) {
          throw error
        }
        localStorage.removeItem(storageKey)
        console.info('[oss-upload] 旧上传会话不可用，重新初始化')
        if (cancelled) {
          throw new UploadCancelledError()
        }
      }
    }

    const initRsp = await init(file, fingerprint, totalParts)
    if (cancelled) {
      await safeAbortMultipart(initRsp.uploadId)
      localStorage.removeItem(storageKey)
      throw new UploadCancelledError()
    }
    uploadId = initRsp.uploadId
    applyUploadedParts(initRsp.uploadedParts || [], initRsp.partSize)
    localStorage.setItem(storageKey, uploadId)
    console.info('[oss-upload] 初始化 Multipart 上传', uploadId, totalParts)
  }

  const refreshMultipartStatus = async () => {
    if (!uploadId) {
      return
    }
    const status = await getMultipartStatus(uploadId)
    applyUploadedParts(status.uploadedParts || [], status.partSize)
    needsStatusRefresh = false
  }

  const runDirect = async (): Promise<string> => {
    while (true) {
      if (cancelled) {
        throw new UploadCancelledError()
      }
      await waitForResume()
      if (cancelled) {
        throw new UploadCancelledError()
      }

      const controller = new AbortController()
      currentAbortController = controller
      try {
        const presign = await presignDirectUpload({
          fileName: file.name,
          fileSize: file.size,
          contentType: file.type || 'application/octet-stream'
        })
        if (paused || cancelled) {
          continue
        }
        await putWithProgress(
          presign.uploadUrl,
          file,
          file.type || 'application/octet-stream',
          controller.signal,
          loaded => notifyProgress(loaded, file.size)
        )
        notifyProgress(file.size, file.size)
        console.info('[oss-upload] 小文件直传完成', file.name)
        return presign.fileUrl
      } catch (error) {
        if (error instanceof UploadPausedError) {
          if (cancelled) {
            throw new UploadCancelledError()
          }
          continue
        }
        throw error
      } finally {
        if (currentAbortController === controller) {
          currentAbortController = null
        }
      }
    }
  }

  const runMultipart = async (): Promise<string> => {
    await ensureMultipartSession()

    while (true) {
      if (cancelled) {
        throw new UploadCancelledError()
      }
      await waitForResume()
      if (cancelled) {
        throw new UploadCancelledError()
      }

      if (needsStatusRefresh) {
        try {
          await refreshMultipartStatus()
        } catch (error) {
          console.warn('[oss-upload] 查询 Multipart 状态失败，准备重新初始化', error)
          if (storageKey) {
            localStorage.removeItem(storageKey)
          }
          uploadId = null
          uploadedParts = new Set()
          partProgress = new Map()
          await ensureMultipartSession()
        }
      }

      const totalParts = Math.ceil(file.size / currentPartSize)
      const missing = Array.from({ length: totalParts }, (_, index) => index + 1)
        .filter(partNumber => !uploadedParts.has(partNumber))

      if (missing.length === 0) {
        await waitForResume()
        if (cancelled) {
          throw new UploadCancelledError()
        }
        // 后端 complete 会再次 ListParts，以服务端实际分片为准。
        const completed = await completeMultipartUpload(uploadId!, [])
        if (storageKey) {
          localStorage.removeItem(storageKey)
        }
        notifyProgress(file.size, file.size)
        console.info('[oss-upload] Multipart 上传完成', completed.url)
        return completed.url
      }

      const urlResponse = await getPartUrls(uploadId!, missing)
      const urlMap = new Map(urlResponse.urls.map(item => [item.partNumber, item.url]))
      const controller = new AbortController()
      currentAbortController = controller

      try {
        await runWithConcurrency(
          missing.map(partNumber => async () => {
            if (paused || cancelled) {
              throw new UploadPausedError()
            }
            const url = urlMap.get(partNumber)
            if (!url) {
              throw new Error(`缺少分片 ${partNumber} 的预签名 URL`)
            }
            const part = await withRetry(() => uploadPart(
              file,
              partNumber,
              url,
              controller.signal,
              currentPartSize,
              loaded => {
                partProgress.set(partNumber, loaded)
                notifyLoaded()
              }
            ))
            uploadedParts.add(partNumber)
            partProgress.set(partNumber, part.size)
            notifyLoaded()
            console.info('[oss-upload] 分片上传成功', partNumber, part.size, file.size)
            return part
          }),
          MAX_CONCURRENT_PARTS
        )
      } catch (error) {
        if (error instanceof UploadPausedError) {
          if (cancelled) {
            throw new UploadCancelledError()
          }
          needsStatusRefresh = true
          continue
        }
        throw error
      } finally {
        if (currentAbortController === controller) {
          currentAbortController = null
        }
      }
    }
  }

  const run = async (): Promise<string> => {
    if (cancelled) {
      throw new UploadCancelledError()
    }
    console.info(
      useMultipart ? '[oss-upload] 使用 Multipart 上传' : '[oss-upload] 使用小文件直传',
      file.name,
      file.size
    )
    return useMultipart ? runMultipart() : runDirect()
  }

  const start = () => {
    if (!promise) {
      started = true
      promise = run()
    }
    return promise
  }

  const pause = () => {
    // 小文件没有分片和 uploadId，暂停后无法从中间继续，因此直接忽略暂停。
    if (!useMultipart || cancelled || paused || !started) {
      return
    }
    paused = true
    currentAbortController?.abort()
  }

  const resume = () => {
    if (cancelled) {
      return Promise.reject(new UploadCancelledError())
    }
    if (!paused) {
      return start()
    }
    paused = false
    wakeResumeWaiters()
    return start()
  }

  const cancel = async () => {
    if (cancelled) {
      return
    }
    cancelled = true
    paused = false
    wakeResumeWaiters()
    currentAbortController?.abort()
    if (storageKey) {
      localStorage.removeItem(storageKey)
    }
    if (useMultipart && uploadId) {
      await safeAbortMultipart(uploadId)
    }
  }

  return {
    file,
    start,
    pause,
    resume,
    cancel,
    isPaused: () => paused,
    isCancelled: () => cancelled
  }
}

/**
 * 兼容旧调用：上传完成后返回文件访问地址。
 */
export async function uploadFileSmart(
  file: File,
  onProgress?: (progress: UploadProgress) => void
): Promise<string> {
  return createUploadTask(file, onProgress).start()
}

async function init(file: File, fingerprint: string, totalParts: number) {
  return initMultipartUpload({
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type || 'application/octet-stream',
    fileFingerprint: fingerprint,
    partSize: PART_SIZE,
    totalParts
  })
}

async function uploadPart(
  file: File,
  partNumber: number,
  url: string,
  signal: AbortSignal,
  partSize: number,
  onProgress: (loaded: number) => void
) {
  const start = (partNumber - 1) * partSize
  const end = Math.min(start + partSize, file.size)
  const blob = file.slice(start, end)

  try {
    const response = await putWithProgress(
      url,
      blob,
      file.type || 'application/octet-stream',
      signal,
      onProgress
    )
    if (!response.etag) {
      throw new Error(`分片 ${partNumber} 缺少 ETag`)
    }
    return {
      partNumber,
      etag: response.etag,
      size: end - start
    }
  } catch (error) {
    if (error instanceof UploadPausedError) {
      throw error
    }
    const message = error instanceof Error ? error.message : '上传失败'
    throw new Error(`分片 ${partNumber} 上传失败: ${message}`)
  }
}

/**
 * 使用 XMLHttpRequest 执行 PUT。
 *
 * <p>选择 XHR 而不是 fetch 的原因：
 * 1. 可以读取 upload.onprogress；
 * 2. 可以通过 abort() 中止请求；
 * 3. 可以读取 ETag 响应头。
 */
function putWithProgress(
  url: string,
  body: Blob | File,
  contentType: string,
  signal: AbortSignal,
  onProgress: (loaded: number) => void
): Promise<{ etag: string | null }> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new UploadPausedError())
      return
    }

    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url)
    xhr.setRequestHeader('Content-Type', contentType)

    xhr.upload.onprogress = event => {
      if (event.lengthComputable) {
        onProgress(event.loaded)
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({ etag: xhr.getResponseHeader('ETag') })
        return
      }
      reject(new Error(`上传失败: ${xhr.status}`))
    }

    xhr.onerror = () => reject(new Error('网络错误，上传失败'))
    xhr.onabort = () => reject(new UploadPausedError())

    const abortHandler = () => xhr.abort()
    signal.addEventListener('abort', abortHandler, { once: true })
    xhr.onloadend = () => signal.removeEventListener('abort', abortHandler)
    xhr.send(body)
  })
}

async function withRetry<T>(fn: () => Promise<T>): Promise<T> {
  let lastError: unknown
  for (let i = 0; i < MAX_RETRY; i++) {
    try {
      return await fn()
    } catch (error) {
      if (error instanceof UploadPausedError) {
        throw error
      }
      lastError = error
      console.warn(`[oss-upload] 分片上传失败，准备第 ${i + 1} 次重试`, error)
      await new Promise(resolve => setTimeout(resolve, 300 * (i + 1)))
    }
  }
  throw lastError
}

/**
 * 并发执行分片任务。
 *
 * <p>与直接使用 Promise.all 不同：某个分片失败后不再启动新任务，
 * 但仍然等待已经启动的任务结束，避免暂停/取消时出现后台残留请求。
 */
async function runWithConcurrency<T>(
  tasks: Array<() => Promise<T>>,
  limit: number
): Promise<T[]> {
  const results: T[] = []
  let cursor = 0
  let firstError: unknown
  let hasError = false

  async function worker() {
    while (cursor < tasks.length) {
      if (hasError) {
        return
      }
      const index = cursor++
      try {
        results[index] = await tasks[index]()
      } catch (error) {
        if (!hasError) {
          firstError = error
          hasError = true
        }
        return
      }
    }
  }

  await Promise.all(Array.from({ length: Math.min(limit, tasks.length) }, worker))
  if (hasError) {
    throw firstError
  }
  return results
}

async function buildFingerprint(file: File): Promise<string> {
  const raw = `${file.name}|${file.size}|${file.lastModified}`
  // Web Crypto 只在 HTTPS 或 localhost 等安全上下文可用。
  // 当前生产页面使用 http://服务器IP:8087，因此 crypto.subtle 可能为 undefined。
  if (typeof crypto === 'undefined' || !crypto.subtle) {
    console.warn('[oss-upload] 当前不是安全上下文，使用降级文件指纹算法')
    return simpleHash(raw)
  }

  const bytes = new TextEncoder().encode(raw)
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return Array.from(new Uint8Array(digest))
    .map(value => value.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * 非安全上下文下的轻量指纹算法。
 *
 * <p>这里只用于识别“同一用户重新选择同一文件”，不需要密码学强度。
 * 使用 FNV-1a 对文件名、大小、修改时间做稳定哈希，保证刷新页面后仍能得到相同值。
 */
function simpleHash(input: string): string {
  let hash = 2166136261
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return (hash >>> 0).toString(16).padStart(8, '0')
}

function readUploadId(storageKey: string): string {
  return localStorage.getItem(storageKey) || ''
}

/**
 * 兼容旧调用：只取消服务端 Multipart 会话。
 *
 * <p>新的 UI 应优先使用 createUploadTask().cancel()，
 * 因为它会同时中止浏览器请求、清理 localStorage 并调用 abort 接口。
 */
export async function cancelMultipartUpload(uploadId: string) {
  await abortMultipartUpload(uploadId)
}
