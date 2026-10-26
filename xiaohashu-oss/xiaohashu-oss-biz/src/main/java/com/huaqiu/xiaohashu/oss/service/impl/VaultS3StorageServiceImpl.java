package com.huaqiu.xiaohashu.oss.service.impl;

import com.huaqiu.xiaohashu.oss.config.MinioProperties;
import com.huaqiu.xiaohashu.oss.service.VaultS3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.time.Duration;
import java.util.List;

/**
 * AWS SDK v2 实现：VaultS3 直传预签名与 S3 Multipart。
 */
@Service
@Slf4j
public class VaultS3StorageServiceImpl implements VaultS3StorageService {

    private static final Duration URL_EXPIRE = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioProperties properties;

    public VaultS3StorageServiceImpl(S3Client s3Client,
                                     S3Presigner s3Presigner,
                                     MinioProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public String presignPutObject(String objectName) {
        log.info("生成小文件 PUT 预签名 URL, objectName={}", objectName);
        return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(URL_EXPIRE)
                        .putObjectRequest(builder -> builder
                                .bucket(properties.getBucketName())
                                .key(objectName))
                        .build())
                .url()
                .toString();
    }

    @Override
    public String createMultipartUpload(String objectName, String contentType) {
        log.info("创建 S3 Multipart Upload, objectName={}, contentType={}", objectName, contentType);
        String uploadId = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(objectName)
                        .contentType(contentType)
                        .build())
                .uploadId();
        log.info("S3 Multipart Upload 创建成功, objectName={}, uploadId={}", objectName, uploadId);
        return uploadId;
    }

    @Override
    public String presignUploadPart(String objectName, String uploadId, int partNumber) {
        log.info("生成分片预签名 URL, objectName={}, uploadId={}, partNumber={}",
                objectName, uploadId, partNumber);
        return s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                        .signatureDuration(URL_EXPIRE)
                        .uploadPartRequest(UploadPartRequest.builder()
                                .bucket(properties.getBucketName())
                                .key(objectName)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build())
                        .build())
                .url()
                .toString();
    }

    @Override
    public List<UploadedPart> listParts(String objectName, String uploadId) {
        List<UploadedPart> parts = s3Client.listParts(ListPartsRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(objectName)
                        .uploadId(uploadId)
                        .build())
                .parts()
                .stream()
                .map(part -> new UploadedPart(part.partNumber(), part.eTag(), part.size()))
                .toList();
        log.info("查询 S3 Multipart 已上传分片, objectName={}, uploadId={}, partCount={}",
                objectName, uploadId, parts.size());
        return parts;
    }

    @Override
    public void completeMultipartUpload(String objectName, String uploadId, List<UploadedPart> parts) {
        log.info("完成 S3 Multipart Upload, objectName={}, uploadId={}, partCount={}",
                objectName, uploadId, parts.size());
        List<CompletedPart> completedParts = parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.etag())
                        .build())
                .toList();

        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectName)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build());
    }

    @Override
    public void abortMultipartUpload(String objectName, String uploadId) {
        log.warn("取消 S3 Multipart Upload, objectName={}, uploadId={}", objectName, uploadId);
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectName)
                .uploadId(uploadId)
                .build());
    }
}
