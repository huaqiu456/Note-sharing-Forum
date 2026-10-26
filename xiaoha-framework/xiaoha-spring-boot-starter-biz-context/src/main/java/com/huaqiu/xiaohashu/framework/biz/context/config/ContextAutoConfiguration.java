package com.huaqiu.xiaohashu.framework.biz.context.config;

import com.huaqiu.xiaohashu.framework.biz.context.interceptor.AddUserId2RequestInterceptor;
import com.huaqiu.xiaohashu.framework.biz.context.interceptor.UserIdInterceptor;
import org.aopalliance.intercept.Interceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class ContextAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    public UserIdInterceptor userIdInterceptor() {
        return new UserIdInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdInterceptor());
    }

    @Bean
    public AddUserId2RequestInterceptor addUserId2RequestInterceptor(){
        return new AddUserId2RequestInterceptor();
    }

}
