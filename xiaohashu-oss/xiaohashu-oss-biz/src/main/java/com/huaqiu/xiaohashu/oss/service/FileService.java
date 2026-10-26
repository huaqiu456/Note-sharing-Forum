package com.huaqiu.xiaohashu.oss.service;

import com.huaqiu.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    Response<?> uploadFile(MultipartFile file);

}
