package com.huaqiu.xiaohashu.oss.api;


import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.oss.config.FeignFormConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "xiaohashu-oss", configuration = FeignFormConfig.class)
public interface OssFileFeignClient {

    String PREFIX = "/file";

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping(value = PREFIX + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file);

}