package com.huaqiu.xiaohashu.auth.domain.dto;

import com.huaqiu.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class UserLoginReqDto implements Serializable {

    @PhoneNumber
    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String code;

    private String password;

    @NotNull(message = "登录类型不能为空")
    private Integer type;


}
