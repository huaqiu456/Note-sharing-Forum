package com.huaqiu.xiaohashu.auth.controller;

import com.huaqiu.framework.biz.operationlog.aspect.ApiOperationLog;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.auth.domain.dto.SendUpdatePasswordDto;
import com.huaqiu.xiaohashu.auth.domain.dto.UserLoginReqDto;
import com.huaqiu.xiaohashu.auth.interceptor.LoginUserContextHolder;
import com.huaqiu.xiaohashu.auth.service.interfaces.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperationLog(description = "用户登录/注册")
    @PostMapping("/login")
    public Response<String> loginAndRegister(@Validated @RequestBody UserLoginReqDto userLoginReqDto){
        return userService.loginandRegister(userLoginReqDto);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("ThreadLocal 传递UserId");

        return userService.logout(Long.valueOf(userId));
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description="修改用户密码")
    public Response<?> updatePassword(@Validated @RequestBody SendUpdatePasswordDto sendUpdatePasswordDto){
        Long userId = LoginUserContextHolder.getUserId();

        return userService.updatePassword(userId,sendUpdatePasswordDto);
    }

}
