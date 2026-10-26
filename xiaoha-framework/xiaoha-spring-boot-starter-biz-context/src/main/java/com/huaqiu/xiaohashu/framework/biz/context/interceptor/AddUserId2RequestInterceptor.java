package com.huaqiu.xiaohashu.framework.biz.context.interceptor;

import com.huaqiu.framework.common.constant.GlobalConstants;
import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class AddUserId2RequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Long userId = LoginUserContextHolder.getUserId();
        System.out.println("openfeign 调用前设置请求头");
        if (Objects.nonNull(userId)) {
            requestTemplate.header(GlobalConstants.USER_ID, String.valueOf(userId));
            log.info("########## feign 请求设置请求头 userId: {}", userId);
        }
    }
}
