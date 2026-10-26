package com.huaqiu.xiaohashu.user.api;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "xiaohashu-user")
public interface UserFeignClient {

    @PostMapping(value = "/user/findById")
    public Response<FindUserInfoRspDto> findUserById(@RequestBody FindUserInfoDto findUserInfoDto);
}
