package com.huaqiu.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.collect.Lists;
import com.huaqiu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import com.huaqiu.framework.common.enums.DeleteEnum;
import com.huaqiu.framework.common.enums.StatusEnum;
import com.huaqiu.framework.common.exception.BizException;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.framework.common.util.JsonUtils;
import com.huaqiu.xiaohashu.auth.constant.RedisKeyConstants;
import com.huaqiu.xiaohashu.auth.constant.RoleConstants;
import com.huaqiu.xiaohashu.auth.domain.dto.SendUpdatePasswordDto;
import com.huaqiu.xiaohashu.auth.domain.dto.UserLoginReqDto;
import com.huaqiu.xiaohashu.auth.domain.po.User;
import com.huaqiu.xiaohashu.auth.domain.po.UserRole;
import com.huaqiu.xiaohashu.auth.enums.LoginTypeEnum;
import com.huaqiu.xiaohashu.auth.enums.ResponseCodeEnum;
import com.huaqiu.xiaohashu.auth.mapper.UserMapper;
import com.huaqiu.xiaohashu.auth.mapper.UserRoleMapper;
import com.huaqiu.xiaohashu.auth.service.interfaces.SessionAuthService;
import com.huaqiu.xiaohashu.auth.service.interfaces.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    @Resource
    private PasswordEncoder passwordEncoder;

    /** 权限快照写入 Sa-Token 会话（登录时刷新，注销时清理） */
    @Resource
    private SessionAuthService sessionAuthService;

    /**
     * 登录 / 自动注册（验证码登录、密码登录）
     *
     * <p>流程：
     * <ol>
     *   <li>按登录类型校验（验证码 / 密码），并取得或创建 userId</li>
     *   <li>{@code StpUtil.login(userId)} 建立登录态（token + Account-Session）</li>
     *   <li><b>v2 新增</b>：把「角色 + 权限」写进会话（见 {@link SessionAuthService}），
     *       使权限数据与登录态绑定 —— 网关鉴权只读会话，不需要跨服务回源</li>
     * </ol>
     *
     * <p>注意：第 3 步失败**不会**导致登录失败（只记录 ERROR 日志），
     * 用户仍可登录，只是暂时没有任何权限；重新登录即可重建。
     *
     * @param userLoginReqDto 登录请求（手机号、登录类型、验证码或密码）
     * @return 成功时返回 token 字符串
     * @throws BizException 验证码错误 / 密码错误 / 手机号未注册等业务异常
     */
    @Override
    public Response<String> loginandRegister(UserLoginReqDto userLoginReqDto) {
        String phone = userLoginReqDto.getPhone();
        Integer type = userLoginReqDto.getType();

        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);

        Long userId = null;

        // 判断登录类型
        switch (loginTypeEnum) {
            case VERIFICATION_CODE: // 验证码登录
                String verificationCode = userLoginReqDto.getCode();
                // 校验入参验证码是否为空
                if (StringUtils.isBlank(verificationCode)) {
                    return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "验证码不能为空");
                }

                // 构建验证码 Redis Key
                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                // 查询存储在 Redis 中该用户的登录验证码
                String sentCode = (String) redisTemplate.opsForValue().get(key);

                // 判断用户提交的验证码，与 Redis 中的验证码是否一致
                if (!StringUtils.equals(verificationCode, sentCode)) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }

                // 通过手机号查询记录
                User user = userMapper.selectByPhone(phone);

                log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(user));

                // 判断是否注册
                if (Objects.isNull(user)) {
                    // 若此用户还没有注册，系统自动注册该用户
                    // todo
                    userId=registerUser(phone);
                } else {
                    // 已注册，则获取其用户 ID
                    userId = user.getId();
                }
                break;
            case PASSWORD: // 密码登录
                String password = userLoginReqDto.getPassword();
                // 校验入参验证码是否为空
                if (StringUtils.isBlank(password)) {
                    return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "密码不能为空");
                }

                User user1 = userMapper.selectByPhone(phone);


                // 判断是否注册
                if (Objects.isNull(user1)) {
                    // 此用户还没注册，提示用手机号来进行注册
                    throw new BizException(ResponseCodeEnum.VERIFICATION_NOT_REGISTERED);

                } else {
                    // 拿到密文密码
                    String encodePassword = user1.getPassword();

                    // 匹配密码是否一致
                    boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                    if(!isPasswordCorrect){
                        throw new BizException(ResponseCodeEnum.VERIFICATION_PASSWORD_ERROR);
                    }
                    userId = user1.getId();
                }
                break;
            default:
                break;
        }

        // SaToken 登录用户，并返回 token 令牌
        // todo
        // SaToken 登录用户, 入参为用户 ID
        StpUtil.login(userId);

        // ===== 权限进会话（v2 核心）=====
        // 登录成功后把「角色 + 权限」算好写进 Sa-Token 会话，与登录态绑定：
        //   - 网关鉴权时只读会话，不再跨服务回源（消除 Feign/WebClient 那条链路）
        //   - Redis 丢失时会话与 token 一起丢，用户重新登录即自动重建权限
        // 这里刻意兜底：权限写入失败**不能让登录失败**（用户仍可登录，只是暂无权限），
        // 但必须留下 ERROR 日志，否则会出现「登录成功却什么都做不了」且难以排查。
        try {
            sessionAuthService.refreshSessionAuth(userId);
        } catch (Exception e) {
            log.error("==> 登录后写入会话权限失败, userId: {}", userId, e);
        }

        // 获取 Token 令牌
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 返回 Token 令牌
        return Response.success(tokenInfo.tokenValue);
    }



    /**
     * 注册新用户（在事务中执行）
     *
     * <p>只写数据库，<b>不写任何权限缓存</b>：
     * <ul>
     *   <li>{@code t_user}：用户基本信息（id、小红书号、昵称、状态等）</li>
     *   <li>{@code t_user_role_rel}：分配默认角色（common）</li>
     * </ul>
     *
     * <p>「角色 + 权限」由登录成功时经 {@link SessionAuthService} 写入 Sa-Token 会话，
     * 数据库始终是用户角色关系的唯一事实来源。
     *
     * @param phone 手机号
     * @return 新用户 ID；发生异常时返回 null（事务已标记回滚）
     */
    public Long registerUser(String phone){

        return transactionTemplate.execute(status -> {
            try{
                String xiaohashuId =distributedIdGeneratorFeignApi.getSegmentId("leaf-segment-xiaohashu-id");
                Long userId = Long.valueOf(distributedIdGeneratorFeignApi.getSegmentId("leaf-segment-user-id"));
                User user = User.builder()
                        .id(userId)
                        .xiaohashuId(xiaohashuId)
                        .phone(phone)
                        .xiaohashuId(String.valueOf(xiaohashuId)) // 自动生成小红书号 ID
                        .nickname("小红薯" + xiaohashuId) // 自动生成昵称, 如：小红薯10000
                        .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeleteEnum.NO.getValue()) // 逻辑删除
                        .build();
                userMapper.insert(user);

                // 给该用户分配一个默认角色
                UserRole userRole = UserRole.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeleteEnum.NO.getValue())
                        .build();
                userRoleMapper.insert(userRole);


                // 说明：注册这里只写数据库（t_user + t_user_role_rel），不写任何权限缓存。
                //   - 用户 → 角色 以 t_user_role_rel 为唯一数据源
                //   - 角色 + 权限 由「登录成功」时经 SessionAuthService 写入 Sa-Token 会话
                //   - 网关鉴权只读会话，既不查库也不调用其它服务
                return userId;

            } catch (Exception e) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("==> 系统注册用户异常: ", e);
                return null;
            }
        });

    }

    /**
     * 注销登录
     *
     * <p>先 {@code StpUtil.logout(userId)} 让 token 失效，再清理会话中的权限快照。
     * 清理失败不影响注销结果（会话本身设有 TTL，最终会被 Redis 回收）。
     *
     * @param userId 用户 ID
     * @return 固定成功响应
     */
    @Override
    public Response<?> logout(Long userId) {
        StpUtil.logout(userId);
        // 注销后清理会话中的权限快照（会话本身也已设 TTL，这里是及时回收）
        try {
            sessionAuthService.clearSessionAuth(userId);
        } catch (Exception e) {
            log.error("==> 注销后清理会话权限失败, userId: {}", userId, e);
        }
        return Response.success();
    }

    /**
     * 修改密码
     *
     * <p>用 BCrypt 加密后落库（只更新 password 与 updateTime 两个字段）。
     * 密码变更不影响已写入会话的角色与权限。
     *
     * @param userId                用户 ID
     * @param sendUpdatePasswordDto 新密码
     * @return 固定成功响应
     */
    @Override
    public Response<?> updatePassword(Long userId, SendUpdatePasswordDto sendUpdatePasswordDto) {
        String password = sendUpdatePasswordDto.getPassword();

        String encodePassword = passwordEncoder.encode(password);

        User user =  User.builder().id(userId).password(encodePassword).updateTime(LocalDateTime.now()).build();

        userMapper.updateByPrimaryKeySelective(user);

        return Response.success();
    }


}
