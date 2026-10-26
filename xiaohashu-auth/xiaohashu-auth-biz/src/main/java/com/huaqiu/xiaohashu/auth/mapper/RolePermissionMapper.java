package com.huaqiu.xiaohashu.auth.mapper;

import com.huaqiu.xiaohashu.auth.domain.po.RolePermission;
import org.apache.ibatis.annotations.Param;


import java.util.List;

public interface RolePermissionMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RolePermission record);

    int insertSelective(RolePermission record);

    RolePermission selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RolePermission record);

    int updateByPrimaryKey(RolePermission record);

    List<RolePermission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}