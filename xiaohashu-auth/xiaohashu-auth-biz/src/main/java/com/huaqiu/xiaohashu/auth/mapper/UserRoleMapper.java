package com.huaqiu.xiaohashu.auth.mapper;

import com.huaqiu.xiaohashu.auth.domain.po.UserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserRole record);

    int insertSelective(UserRole record);

    UserRole selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRole record);

    int updateByPrimaryKey(UserRole record);

    /**
     * 按用户 ID 查询其角色 ID 列表（权限回源用）
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
