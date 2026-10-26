package com.huaqiu.xiaohashu.oss.service;

import com.huaqiu.xiaohashu.oss.domain.dto.CompleteMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.CompleteMultipartUploadRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.GetPartUrlsReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.GetPartUrlsRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.InitMultipartUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.InitMultipartUploadRspDto;
import com.huaqiu.xiaohashu.oss.domain.dto.MultipartUploadStatusRspDto;

public interface MultipartUploadService {

    InitMultipartUploadRspDto init(InitMultipartUploadReqDto req);

    GetPartUrlsRspDto getPartUrls(GetPartUrlsReqDto req);

    MultipartUploadStatusRspDto status(String uploadId);

    CompleteMultipartUploadRspDto complete(CompleteMultipartUploadReqDto req);

    void abort(String uploadId);
}
