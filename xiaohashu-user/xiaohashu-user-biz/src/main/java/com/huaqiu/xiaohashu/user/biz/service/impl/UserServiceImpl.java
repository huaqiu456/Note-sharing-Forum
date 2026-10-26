package com.huaqiu.xiaohashu.user.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.google.common.base.Preconditions;

import com.huaqiu.framework.common.response.Response;
import com.huaqiu.framework.common.util.JsonUtils;
import com.huaqiu.framework.common.util.ParamUtils;

import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import com.huaqiu.xiaohashu.oss.api.OssFileFeignClient;
import com.huaqiu.xiaohashu.user.biz.domain.dto.UpdateUserInfoDto;
import com.huaqiu.xiaohashu.user.biz.domain.po.User;
import com.huaqiu.xiaohashu.user.biz.enums.ResponseCodeEnum;
import com.huaqiu.xiaohashu.user.biz.enums.SexEnum;

import com.huaqiu.xiaohashu.user.biz.helper.UserCacheHelper;
import com.huaqiu.xiaohashu.user.biz.mapper.UserMapper;
import com.huaqiu.xiaohashu.user.biz.service.UserService;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoDto;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @version: v1.0.0
 * @description: 用户业务
 **/
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userDOMapper;


    @Resource
    private OssFileFeignClient ossFileFeignClient;

    @Resource(name="taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;



    @Resource
    private UserCacheHelper userCacheHelper;

    /**
     * 更新用户信息
     *
     * @param
     * @return
     */
    @Override
    public Response<?> updateUserInfo(UpdateUserInfoDto updateUserInfoDto) {

        User user = new User();
        // 设置当前需要更新的用户 ID
        user.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoDto.getAvatar();

        if (Objects.nonNull(avatarFile)) {
            // todo: 调用对象存储服务上传文件
            System.out.println("上传头像文件");
            String avatarurl=(String)ossFileFeignClient.uploadFile(avatarFile).getData();

            System.out.println("上传头像地址为"+avatarurl);
            user.setAvatar(avatarurl);
            needUpdate=true;
        }

        // 昵称
        String nickname = updateUserInfoDto.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            user.setNickname(nickname);
            needUpdate = true;
        }

        // 小哈书号
        String xiaohashuId = updateUserInfoDto.getXiaohashuId();
        if (StringUtils.isNotBlank(xiaohashuId)) {
            Preconditions.checkArgument(ParamUtils.checkXiaohashuId(xiaohashuId), ResponseCodeEnum.XIAOHASHU_ID_VALID_FAIL.getErrorMessage());
            user.setXiaohashuId(xiaohashuId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoDto.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            user.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoDto.getBirthday();
        if (Objects.nonNull(birthday)) {
            user.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoDto.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            user.setIntroduction(introduction);
            needUpdate = true;
        }

        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoDto.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            // todo: 调用对象存储服务上传文件
            System.out.println("上传背景文件");
            String backgroundImgFilerurl=(String)ossFileFeignClient.uploadFile(backgroundImgFile).getData();

            System.out.println("上传背景地址为"+backgroundImgFilerurl);
            user.setBackgroundImg(backgroundImgFilerurl);
            needUpdate=true;
        }


        if (needUpdate) {
            // 更新用户信息
            user.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(user);
            // 清理用户信息缓存，避免 /user/me 读到旧数据
            userCacheHelper.delete(user.getId());
        }
        return Response.success();
    }

    @Override
    public Response<FindUserInfoRspDto> findUserById(FindUserInfoDto findUserInfoDto) {

        Long userId = findUserInfoDto.getUserId();

        // ======================
        // 1️⃣ 查缓存
        // ======================
        String userString = userCacheHelper.get(userId);

        if (StringUtils.isNotBlank(userString)) {

            // 命中空缓存（防穿透）
            if ("NULL".equals(userString)) {
                return Response.fail("用户不存在");
            }

            try {
                FindUserInfoRspDto cacheObj =
                        JsonUtils.parseObject(userString, FindUserInfoRspDto.class);
                return Response.success(cacheObj);
            } catch (Exception e) {
                log.error("缓存反序列化失败", e);
            }
        }

        // ======================
        // 2️⃣ 查数据库
        // ======================
        User user = userDOMapper.selectByPrimaryKey(userId);

        if (Objects.isNull(user)) {
            // 写空缓存（防穿透）
            userCacheHelper.set(userId, "NULL", 300);
            return Response.fail("用户不存在");
        }

        // ======================
        // 3️⃣ 构建返回对象（⚠️ 只赋值一次）
        // ======================
        FindUserInfoRspDto rsp = FindUserInfoRspDto.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .xiaohashuId(user.getXiaohashuId())
                .sex(user.getSex())
                .birthday(user.getBirthday())
                .introduction(user.getIntroduction())
                .backgroundImg(user.getBackgroundImg())
                .build();

        // ======================
        // 4️⃣ 异步写缓存
        // ======================
        taskExecutor.submit(() -> {
            try {
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                String json = JsonUtils.toJsonString(rsp);
                userCacheHelper.set(userId, json, expireSeconds);
            } catch (Exception e) {
                log.error("缓存写入失败 userId={}", userId, e);
            }
        });

        // ======================
        // 5️⃣ 返回结果
        // ======================
        return Response.success(rsp);
    }

    @Override
    public Response<FindUserInfoRspDto> findCurrentUser() {
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            return Response.fail("未登录");
        }
        FindUserInfoDto dto = FindUserInfoDto.builder().userId(userId).build();
        return findUserById(dto);
    }
}


