package com.huaqiu.xiaohashu.user.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserInfoRspDto {

    private Long userId;

    private String nickname;

    private String avatar;

    private String xiaohashuId;

    private Integer sex;

    private LocalDate birthday;

    private String introduction;

    private String backgroundImg;

}
