package com.huaqiu.xiaohashu.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.huaqiu.framework.common.exception.BizException;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.xiaohashu.auth.config.sms.AliyunSmsHelper;
import com.huaqiu.xiaohashu.auth.constant.RedisKeyConstants;
import com.huaqiu.xiaohashu.auth.domain.dto.SendVerificationCodeDto;
import com.huaqiu.xiaohashu.auth.enums.ResponseCodeEnum;
import com.huaqiu.xiaohashu.auth.service.interfaces.VerificationCodeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource(name ="taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private AliyunSmsHelper aliyunSmsHelper;


    @Override
    public Response<?> send(SendVerificationCodeDto sendVerificationCodeDto) {

        String phone = sendVerificationCodeDto.getPhone();

        String key = RedisKeyConstants.buildVerificationCodeKey(phone);

        boolean isSent = redisTemplate.hasKey(key);
        if(isSent){
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        String verificationCode = RandomUtil.randomNumbers(6);
        log.info("==> 手机号: {}, 已生成验证码：【{}】", phone, verificationCode);
        // todo

        threadPoolTaskExecutor.submit(()->
        {
           String signName ="速通互联验证码";
           String templateCode = "100001";
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"3\"}", verificationCode);
            aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);

        });

        log.info("==> 手机号: {}, 已发送验证码：【{}】", phone, verificationCode);

        redisTemplate.opsForValue().set(key,verificationCode,3, TimeUnit.MINUTES);

        return Response.success();


    }
}
