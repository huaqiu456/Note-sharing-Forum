package com.huaqiu.xiaohashu.note.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.user.api.UserFeignClient;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Component
@Slf4j
public class UserRpcService {

    @Resource
    private UserFeignClient userFeignClient;

    @Autowired
    private ObjectMapper objectMapper;  // 备用方案

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息，失败返回 null
     */
    public FindUserInfoRspDto findUserInfo(Long userId) {
        // 1. 参数校验
        if (userId == null || userId <= 0) {
            log.warn("findUserInfo 参数无效: userId={}", userId);
            return null;
        }

        try {
            // 2. 构建请求参数
            FindUserInfoDto findUserInfoDto = FindUserInfoDto.builder()
                    .userId(userId)
                    .build();

            // 3. 调用 Feign 接口（使用泛型）
            Response<FindUserInfoRspDto> response = userFeignClient.findUserById(findUserInfoDto);

            // 4. 检查响应
            if (response == null) {
                log.error("调用用户服务返回 null，userId: {}", userId);
                return null;
            }

            // 5. 检查业务状态码
            if (!response.isSuccess()) {
                log.error("调用用户服务失败，userId: {}, code: {}, message: {}",
                        userId, response.getErrorCode(), response.getMessage());
                return null;
            }

            // 6. 获取数据（类型正确，无需强转）
            FindUserInfoRspDto userInfo = response.getData();

            // 7. 检查数据
            if (userInfo == null) {
                log.warn("用户信息不存在，userId: {}", userId);
                return null;
            }

            log.info("获取用户信息成功，userId: {}, username: {}",
                    userId, userInfo.getNickname());
            return userInfo;

        } catch (Exception e) {
            log.error("调用用户服务异常，userId: {}", userId, e);
            return null;
        }
    }
}
