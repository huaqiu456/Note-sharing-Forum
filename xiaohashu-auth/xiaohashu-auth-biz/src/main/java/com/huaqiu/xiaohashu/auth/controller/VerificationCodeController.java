package com.huaqiu.xiaohashu.auth.controller;

import com.huaqiu.framework.biz.operationlog.aspect.ApiOperationLog;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.auth.domain.dto.SendVerificationCodeDto;
import com.huaqiu.xiaohashu.auth.service.impl.VerificationCodeServiceImpl;
import com.huaqiu.xiaohashu.auth.service.interfaces.VerificationCodeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class VerificationCodeController {


    @Resource
    private VerificationCodeService verificationCodeService;

    @PostMapping("/verification/code/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> send(@Validated @RequestBody SendVerificationCodeDto sendVerificationCodeDto){
        return verificationCodeService.send(sendVerificationCodeDto);
    }

}
