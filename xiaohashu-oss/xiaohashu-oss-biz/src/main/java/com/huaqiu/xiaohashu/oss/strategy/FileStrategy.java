package com.huaqiu.xiaohashu.oss.strategy;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStrategy {

    String uploadFile(MultipartFile file,String bucketName) ;

}
