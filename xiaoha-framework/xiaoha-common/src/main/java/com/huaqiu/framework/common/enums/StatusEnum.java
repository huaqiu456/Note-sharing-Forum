package com.huaqiu.framework.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 状态
 **/
@Getter
@AllArgsConstructor
public enum StatusEnum {
    ENABLE((byte)0),
    DISABLED((byte)1);

    private final Byte value;
}
