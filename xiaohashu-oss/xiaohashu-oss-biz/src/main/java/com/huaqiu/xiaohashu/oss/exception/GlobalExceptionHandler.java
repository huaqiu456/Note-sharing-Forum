package com.huaqiu.xiaohashu.oss.exception;

import com.huaqiu.framework.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * OSS 服务统一异常响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<?> handleIllegalArgument(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("{} request error: {}", request.getRequestURI(), e.getMessage());
        return Response.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response<?> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error: ", request.getRequestURI(), e);
        return Response.fail("系统繁忙，请稍后再试");
    }
}
