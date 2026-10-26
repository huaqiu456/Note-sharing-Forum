package com.huaqiu.xiaohashu.user.biz.service;


import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.user.biz.domain.dto.UpdateUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;

public interface UserService {

    public Response<?> updateUserInfo(UpdateUserInfoDto updateUserInfoDto);

    public Response<FindUserInfoRspDto> findUserById(FindUserInfoDto findUserInfoDto);

    public Response<FindUserInfoRspDto> findCurrentUser();
}
