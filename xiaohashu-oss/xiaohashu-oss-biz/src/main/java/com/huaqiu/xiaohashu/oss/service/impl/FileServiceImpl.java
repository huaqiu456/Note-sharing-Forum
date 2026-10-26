package com.huaqiu.xiaohashu.oss.service.impl;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.oss.service.FileService;
import com.huaqiu.xiaohashu.oss.strategy.FileStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    @Resource
    private FileStrategy fileStrategy;

    @Override
    public Response<?> uploadFile(MultipartFile file) {
        String bucketName = "xiaohashu";
        String url = fileStrategy.uploadFile(file,bucketName);
        return Response.success(url);
    }
}
