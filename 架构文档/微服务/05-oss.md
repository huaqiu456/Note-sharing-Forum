# 对象存储服务 xiaohashu-oss

## 基本信息

| 项 | 值 |
| --- | --- |
| 端口 | 8081 |
| 服务名 | xiaohashu-oss |
| 依赖 | Nacos、MinIO（vaults3，S3 协议兼容） |

## 职责

1. 接收文件上传（图片/视频）。
2. 按策略上传到对象存储。
3. 返回可访问的图片路径。

## 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/file/upload` | 上传文件（multipart/form-data，参数名 `file`） |

被 user 服务以 Feign（`OssFileFeignClient`）调用，用于头像/背景图上传。

## 设计模式

- **工厂 + 策略**：`FileStrategyFactory` 根据配置 `storage.type` 选择实现。
  - `Minio` → `MinioOssFileStrategyImpl`
  - `Aliyun` → `AliyunOssFileStrategyImpl`
- 配合 Nacos 配置可动态刷新（`@RefreshScope`）。

## 上传流程

1. 校验文件非空。
2. 生成 UUID 作为对象名（保留原后缀）。
3. 调 MinIO 客户端 `putObject` 上传到 bucket `xiaohashu`。
4. 返回**相对路径** `/images/{objectName}`。

## 图片访问方式

返回的是相对路径，由前端 Nginx 反向代理到内部 MinIO：

```
浏览器 /images/xxx.png
   → Nginx(8087)
   → 内部 MinIO(9000) /xiaohashu/xxx.png
```

**优点**：对象存储不需要对公网暴露 9000 端口，也不受内网地址影响。

前提：bucket 需设置为**公开读**。

## 关键类

| 类 | 作用 |
| --- | --- |
| `FileController` | 上传入口 |
| `FileServiceImpl` | 调用策略上传 |
| `FileStrategyFactory` | 按配置选择存储策略 |
| `MinioOssFileStrategyImpl` | MinIO 上传实现 |
| `MinioProperties` | endpoint / accessKey / secretKey / bucketName |
