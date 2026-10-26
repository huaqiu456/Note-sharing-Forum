package com.huaqiu.xiaohashu.oss.service;

import com.huaqiu.xiaohashu.oss.domain.dto.DirectUploadReqDto;
import com.huaqiu.xiaohashu.oss.domain.dto.DirectUploadRspDto;

public interface DirectUploadService {
    DirectUploadRspDto presign(DirectUploadReqDto req);
}
