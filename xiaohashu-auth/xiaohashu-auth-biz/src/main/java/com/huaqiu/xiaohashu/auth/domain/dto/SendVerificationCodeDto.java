package com.huaqiu.xiaohashu.auth.domain.dto;

import com.huaqiu.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendVerificationCodeDto {

    @PhoneNumber
    @NotBlank(message = "手机号不能为空")
    private String phone ;
}
