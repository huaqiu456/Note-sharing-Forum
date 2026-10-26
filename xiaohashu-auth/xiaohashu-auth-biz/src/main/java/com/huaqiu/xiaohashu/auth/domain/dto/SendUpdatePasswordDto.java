package com.huaqiu.xiaohashu.auth.domain.dto;

import com.huaqiu.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.lang.annotation.Before;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendUpdatePasswordDto {

    @NotBlank(message = "新密码不能为空")
    private String password ;
}
