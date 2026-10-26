package com.huaqiu.xiaohashu.oss.service.impl;

import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import com.huaqiu.xiaohashu.oss.config.MinioProperties;
import com.huaqiu.xiaohashu.oss.domain.dto.DirectUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.DirectUploadRspDto;
import com.huaqiu.xiaohashu.oss.service.DirectUploadService;
import com.huaqiu.xiaohashu.oss.service.VaultS3StorageService;
import com.huaqiu.xiaohashu.oss.util.PresignedUrlUtils;
import com.huaqiu.xiaohashu.oss.util.UploadValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 小文件单次预签名上传。
 */
@Service
@Slf4j
public class DirectUploadServiceImpl implements DirectUploadService {

    private static final String PATH_PREFIX = "/vaults3";
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VaultS3StorageService storageService;
    private final MinioProperties properties;

    public DirectUploadServiceImpl(VaultS3StorageService storageService,
                                   MinioProperties properties) {
        this.storageService = storageService;
        this.properties = properties;
    }

    @Override
    public DirectUploadRspDto presign(DirectUploadReqDto req) {
        Long userId = requireUser();
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        UploadValidationUtils.validateDirect(req.getFileSize());
        log.info("小文件直传预签名, userId={}, fileName={}, fileSize={}",
                userId, req.getFileName(), req.getFileSize());

        String objectName = buildObjectName(req.getFileName());
        String rawUrl = storageService.presignPutObject(objectName);
        String browserUrl = PresignedUrlUtils.insertPathPrefix(rawUrl, PATH_PREFIX);
        log.info("小文件直传预签名完成, userId={}, objectName={}", userId, objectName);

        return DirectUploadRspDto.builder()
                .objectName(objectName)
                .uploadUrl(browserUrl)
                .fileUrl("/images/" + objectName)
                .expiresAt(LocalDateTime.now().plusMinutes(15).toString())
                .build();
    }

    private Long requireUser() {
        Long userId = LoginUserContextHolder.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return userId;
    }

    private String buildObjectName(String fileName) {
        String suffix = extractSuffix(fileName);
        return "direct/" + LocalDate.now().format(DATE_PATH) + "/"
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
}
