package com.huaqiu.xiaohashu.oss.service;

import java.util.List;

/**
 * VaultS3 存储能力抽象。
 */
public interface VaultS3StorageService {

    String presignPutObject(String objectName);

    String createMultipartUpload(String objectName, String contentType);

    String presignUploadPart(String objectName, String uploadId, int partNumber);

    List<UploadedPart> listParts(String objectName, String uploadId);

    void completeMultipartUpload(String objectName, String uploadId, List<UploadedPart> parts);

    void abortMultipartUpload(String objectName, String uploadId);

    record UploadedPart(int partNumber, String etag, Long size) {
    }
}
