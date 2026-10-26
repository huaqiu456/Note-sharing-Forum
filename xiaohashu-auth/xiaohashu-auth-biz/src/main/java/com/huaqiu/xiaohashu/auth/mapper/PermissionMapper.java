package com.huaqiu.xiaohashu.auth.mapper;

import com.huaqiu.xiaohashu.auth.domain.po.Permission;
import jakarta.validation.constraints.DecimalMax;

import java.util.List;

public interface PermissionMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Permission record);

    int insertSelective(Permission record);

    Permission selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Permission record);

    int updateByPrimaryKey(Permission record);

    /**
     * 查询 APP 端所有被启用的权限
     *
     * @return
     */
    List<Permission> selectAppEnabledList();
}