package com.huaqiu.xiaohashu.oss.controller;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import com.huaqiu.xiaohashu.oss.domain.dto.AbortMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.CompleteMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.DirectUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.GetPartUrlsReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.InitMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.service.DirectUploadService;
import com.huaqiu.xiaohashu.oss.service.FileService;
import com.huaqiu.xiaohashu.oss.service.MultipartUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    private final FileService fileService;
    private final DirectUploadService directUploadService;
    private final MultipartUploadService multipartUploadService;

    public FileController(FileService fileService,
                          DirectUploadService directUploadService,
                          MultipartUploadService multipartUploadService) {
        this.fileService = fileService;
        this.directUploadService = directUploadService;
        this.multipartUploadService = multipartUploadService;
    }

    @PostMapping(value="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> uploadFile(@RequestPart(value = "file")MultipartFile file){
        log.info("当前用户 ID: {}", LoginUserContextHolder.getUserId());
        return fileService.uploadFile(file);
    }

    @PostMapping("/direct/presign")
    public Response<?> directPresign(@RequestBody DirectUploadReqDto req) {
        log.info("接收到小文件直传预签名请求, fileName={}", req.getFileName());
        return Response.success(directUploadService.presign(req));
    }

    @PostMapping("/multipart/init")
    public Response<?> initMultipart(@RequestBody InitMultipartUploadReqDto req) {
        log.info("接收到 Multipart 初始化请求, fileName={}, fileSize={}",
                req.getFileName(), req.getFileSize());
        return Response.success(multipartUploadService.init(req));
    }

    @PostMapping("/multipart/part-urls")
    public Response<?> getPartUrls(@RequestBody GetPartUrlsReqDto req) {
        log.info("接收到分片 URL 请求, uploadId={}, partNumbers={}",
                req.getUploadId(), req.getPartNumbers());
        return Response.success(multipartUploadService.getPartUrls(req));
    }

    @GetMapping("/multipart/{uploadId}/status")
    public Response<?> multipartStatus(@PathVariable String uploadId) {
        log.info("接收到 Multipart 状态查询请求, uploadId={}", uploadId);
        return Response.success(multipartUploadService.status(uploadId));
    }

    @PostMapping("/multipart/complete")
    public Response<?> completeMultipart(@RequestBody CompleteMultipartUploadReqDto req) {
        log.info("接收到 Multipart 完成请求, uploadId={}", req.getUploadId());
        return Response.success(multipartUploadService.complete(req));
    }

    @PostMapping("/multipart/abort")
    public Response<?> abortMultipart(@RequestBody AbortMultipartUploadReqDto req) {
        log.warn("接收到 Multipart 取消请求, uploadId={}", req.getUploadId());
        multipartUploadService.abort(req.getUploadId());
        return Response.success();
    }
}
