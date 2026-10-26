package com.huaqiu.xiaohashu.user.biz.controller;

import com.huaqiu.framework.biz.operationlog.aspect.ApiOperationLog;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.user.biz.domain.dto.UpdateUserInfoDto;
import com.huaqiu.xiaohashu.user.biz.service.UserService;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping( "/test")
    @ApiOperationLog(description = "测试")
    public Response<?> test(){
        return Response.success("成功");
    }

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(UpdateUserInfoDto updateUserInfoDto){
        return userService.updateUserInfo(updateUserInfoDto);
    }

    @PostMapping(value = "/findById")
    public Response<FindUserInfoRspDto> findUserById(@RequestBody FindUserInfoDto findUserInfoDto){
        log.info("调用finduserByID");
        return userService.findUserById(findUserInfoDto);
    }

    @PostMapping(value = "/me")
    public Response<FindUserInfoRspDto> getCurrentUserInfo(){
        return userService.findCurrentUser();
    }
}
