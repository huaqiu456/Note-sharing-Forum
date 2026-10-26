package com.huaqiu.xiaohashu.auth.service.interfaces;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.auth.domain.dto.SendVerificationCodeDto;

public interface VerificationCodeService {

    Response<?> send(SendVerificationCodeDto sendVerificationCodeDto);
}
