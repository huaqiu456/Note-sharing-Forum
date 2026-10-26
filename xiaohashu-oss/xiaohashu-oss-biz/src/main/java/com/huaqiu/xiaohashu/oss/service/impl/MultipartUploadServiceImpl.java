package com.huaqiu.xiaohashu.oss.service.impl;

import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import com.huaqiu.xiaohashu.oss.config.MinioProperties;
import com.huaqiu.xiaohashu.oss.domain.dto.CompleteMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.CompleteMultipartUploadRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.GetPartUrlsReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.GetPartUrlsRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.InitMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.InitMultipartUploadRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.MultipartUploadStatusRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.UploadPartUrlDto;
import com.huaqiu.xiaohashu.oss.domain.po.OssUploadSession;
import com.huaqiu.xiaohashu.oss.enums.UploadSessionStatusEnum;
import com.huaqiu.xiaohashu.oss.mapper.OssUploadSessionMapper;
import com.huaqiu.xiaohashu.oss.service.MultipartUploadService;
import com.huaqiu.xiaohashu.oss.service.VaultS3StorageService;
import com.huaqiu.xiaohashu.oss.util.PresignedUrlUtils;
import com.huaqiu.xiaohashu.oss.util.UploadValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 大文件 S3 Multipart 上传编排。
 */
@Service
@Slf4j
public class MultipartUploadServiceImpl implements MultipartUploadService {

    private static final String PATH_PREFIX = "/vaults3";
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VaultS3StorageService storageService;
    private final OssUploadSessionMapper sessionMapper;
    private final MinioProperties properties;

    public MultipartUploadServiceImpl(VaultS3StorageService storageService,
                                      OssUploadSessionMapper sessionMapper,
                                      MinioProperties properties) {
        this.storageService = storageService;
        this.sessionMapper = sessionMapper;
        this.properties = properties;
    }

    @Override
    public InitMultipartUploadRspDto init(InitMultipartUploadReqDto req) {
        // 获得用户id
        Long userId = requireUser();
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        // 校验 Multipart Upload 参数。
        UploadValidationUtils.validateMultipart(req.getFileSize(), req.getPartSize(), req.getTotalParts());
        log.info("初始化 Multipart Upload, userId={}, fileName={}, fileSize={}, totalParts={}",
                userId, req.getFileName(), req.getFileSize(), req.getTotalParts());

        //查询是否在 mysql 表中
        OssUploadSession existing = sessionMapper.selectActiveByUserAndFingerprint(
                userId, req.getFileFingerprint(), req.getFileSize());
        // 如果在表中则复用返回已经上传了的分片编号
        if (existing != null && !isExpired(existing)) {
            log.info("复用未完成 Multipart 会话, userId={}, uploadId={}", userId, existing.getUploadId());
            List<Integer> uploaded = storageService.listParts(existing.getObjectName(), existing.getUploadId())
                    .stream()
                    .map(VaultS3StorageService.UploadedPart::partNumber)
                    .toList();
            return toInitResponse(existing, uploaded);
        }
        // 否则过期了或则是还没有这个上传过 就创建上传获得upload id

        String objectName = buildObjectName(req.getFileName());
        String uploadId = storageService.createMultipartUpload(objectName, req.getContentType());
        log.info("Multipart 初始化成功, userId={}, uploadId={}, objectName={}", userId, uploadId, objectName);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(properties.getSessionTtlHours());

        // 然后把这个记录插入数据库中，这个记录包括了upload id作为主键，userid，文件名，filefinger 过期时间等内容

        OssUploadSession session = OssUploadSession.builder()
                .uploadId(uploadId)
                .userId(userId)
                .bucketName(properties.getBucketName())
                .objectName(objectName)
                .fileName(req.getFileName())
                .fileSize(req.getFileSize())
                .contentType(req.getContentType())
                .fileFingerprint(req.getFileFingerprint())
                .partSize(req.getPartSize())
                .totalParts(req.getTotalParts())
                .status(UploadSessionStatusEnum.INIT.getCode())
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        sessionMapper.insert(session);

        return toInitResponse(session, List.of());
    }

    @Override
    public GetPartUrlsRspDto getPartUrls(GetPartUrlsReqDto req) {
        // 得到init之后 可以调用多次getparturl 对每一个 分片都要有一个上传url
        //获取用户id
        Long userId = requireUser();
        log.info("获取分片预签名 URL, userId={}, uploadId={}, partNumbers={}",
                userId, req.getUploadId(), req.getPartNumbers());

        // 要求当前uploadid和用户是对应的 ，并且是合法的上传
        OssUploadSession session = requireSession(req.getUploadId(), userId);
        if (req.getPartNumbers() == null || req.getPartNumbers().isEmpty()) {
            throw new IllegalArgumentException("分片编号不能为空");
        }

        // 针对每一个part 得到一个url
        List<UploadPartUrlDto> urls = req.getPartNumbers().stream().map(partNumber -> {
            validatePartNumber(partNumber, session.getTotalParts());
            String raw = storageService.presignUploadPart(session.getObjectName(), session.getUploadId(), partNumber);
            return UploadPartUrlDto.builder()
                    .partNumber(partNumber)
                    .url(PresignedUrlUtils.insertPathPrefix(raw, PATH_PREFIX))
                    .expiresAt(LocalDateTime.now().plusMinutes(15).toString())
                    .build();
        }).toList();

        // 修改数据库中的状态从init 到uploading中
        if (UploadSessionStatusEnum.INIT.getCode().equals(session.getStatus())) {
            sessionMapper.updateStatus(session.getUploadId(), UploadSessionStatusEnum.UPLOADING.getCode());
        }
        return GetPartUrlsRspDto.builder().urls(urls).build();
    }

    @Override
    public MultipartUploadStatusRspDto status(String uploadId) {
        Long userId = requireUser();
        log.info("查询 Multipart 状态, userId={}, uploadId={}", userId, uploadId);


        OssUploadSession session = requireSession(uploadId, userId);

        // 调用 s3 client 查看已经上传了多少，不能相信mysql数据库
        List<VaultS3StorageService.UploadedPart> parts = storageService.listParts(
                session.getObjectName(), session.getUploadId());
        long uploadedBytes = parts.stream().mapToLong(part -> part.size() == null ? 0L : part.size()).sum();
        return MultipartUploadStatusRspDto.builder()
                .uploadId(session.getUploadId())
                .status(session.getStatus() == null ? "INIT" : toStatusName(session.getStatus()))
                .partSize(session.getPartSize())
                .totalParts(session.getTotalParts())
                .uploadedParts(parts.stream().map(VaultS3StorageService.UploadedPart::partNumber).toList())
                .uploadedBytes(uploadedBytes)
                .totalBytes(session.getFileSize())
                .expiresAt(session.getExpiresAt() == null ? null : session.getExpiresAt().toString())
                .build();
    }

    @Override
    public CompleteMultipartUploadRspDto complete(CompleteMultipartUploadReqDto req) {
        Long userId = requireUser();
        log.info("完成 Multipart Upload, userId={}, uploadId={}", userId, req.getUploadId());
        OssUploadSession session = requireSession(req.getUploadId(), userId);

        // 调用s3 client查看已经上传了的 分片编号，不能相信客户端给的etag是完整的
        List<VaultS3StorageService.UploadedPart> parts = storageService.listParts(
                session.getObjectName(), session.getUploadId());
        // 对比已经上传了的分片编号 是不是和数据库中的数量一致，如果不一致则返回分片不完整
        if (parts.size() != session.getTotalParts()) {
            log.warn("Multipart 分片不完整, uploadId={}, uploaded={}, total={}",
                    session.getUploadId(), parts.size(), session.getTotalParts());
            throw new IllegalArgumentException("分片不完整");
        }
        // 如果确实完成了上传则调用s3client完成上传
        storageService.completeMultipartUpload(session.getObjectName(), session.getUploadId(), parts);

        // 更新数据库状态
        sessionMapper.updateStatus(session.getUploadId(), UploadSessionStatusEnum.COMPLETED.getCode());
        log.info("Multipart Upload 完成, userId={}, uploadId={}, objectName={}",
                userId, session.getUploadId(), session.getObjectName());
        return CompleteMultipartUploadRspDto.builder()
                .url("/images/" + session.getObjectName())
                .objectName(session.getObjectName())
                .size(session.getFileSize())
                .build();
    }

    @Override
    public void abort(String uploadId) {
        Long userId = requireUser();
        log.warn("取消 Multipart Upload, userId={}, uploadId={}", userId, uploadId);
        OssUploadSession session = requireSession(uploadId, userId);
        storageService.abortMultipartUpload(session.getObjectName(), session.getUploadId());
        sessionMapper.updateStatus(session.getUploadId(), UploadSessionStatusEnum.ABORTED.getCode());
    }

    private Long requireUser() {
        Long userId = LoginUserContextHolder.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return userId;
    }

    private OssUploadSession requireSession(String uploadId, Long userId) {
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId 不能为空");
        }
        OssUploadSession session = sessionMapper.selectByUploadId(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("上传会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new IllegalArgumentException("无权操作该上传会话");
        }
        if (isExpired(session)) {
            throw new IllegalArgumentException("上传会话已过期");
        }
        if (UploadSessionStatusEnum.COMPLETED.getCode().equals(session.getStatus())
                || UploadSessionStatusEnum.ABORTED.getCode().equals(session.getStatus())) {
            throw new IllegalArgumentException("上传会话已结束");
        }
        return session;
    }

    private boolean isExpired(OssUploadSession session) {
        return session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private void validatePartNumber(Integer partNumber, Integer totalParts) {
        if (partNumber == null || partNumber < 1 || partNumber > totalParts) {
            throw new IllegalArgumentException("分片编号非法");
        }
    }

    private InitMultipartUploadRspDto toInitResponse(OssUploadSession session, List<Integer> uploadedParts) {
        return InitMultipartUploadRspDto.builder()
                .uploadId(session.getUploadId())
                .objectName(session.getObjectName())
                .partSize(session.getPartSize())
                .totalParts(session.getTotalParts())
                .uploadedParts(uploadedParts)
                .expiresAt(session.getExpiresAt() == null ? null : session.getExpiresAt().toString())
                .build();
    }

    private String buildObjectName(String fileName) {
        String suffix = extractSuffix(fileName);
        return "multipart/" + LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    private String extractSuffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index).replaceAll("[^a-zA-Z0-9.]", "");
    }

    private String toStatusName(Integer status) {
        for (UploadSessionStatusEnum value : UploadSessionStatusEnum.values()) {
            if (value.getCode().equals(status)) {
                return value.name();
            }
        }
        return "UNKNOWN";
    }
}
