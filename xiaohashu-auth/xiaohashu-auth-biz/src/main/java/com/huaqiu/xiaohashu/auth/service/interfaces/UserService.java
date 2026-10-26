package com.huaqiu.xiaohashu.auth.service.interfaces;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.auth.domain.dto.SendUpdatePasswordDto;
import com.huaqiu.xiaohashu.auth.domain.dto.UserLoginReqDto;

public interface UserService {


    Response<String> loginandRegister(UserLoginReqDto userLoginReqDto);

    Response<?> logout(Long userId);

    Response<?> updatePassword(Long userId, SendUpdatePasswordDto sendUpdatePasswordDto);
}
