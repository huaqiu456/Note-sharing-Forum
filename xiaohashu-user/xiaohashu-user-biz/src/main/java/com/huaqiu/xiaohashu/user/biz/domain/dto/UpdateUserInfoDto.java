package com.huaqiu.xiaohashu.user.biz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserInfoDto implements Serializable {

    private String xiaohashuId;

    private String nickname;

    private MultipartFile  avatar;

    private LocalDate birthday;

    private MultipartFile backgroundImg;

    private Integer  sex;

    private String introduction;

}
