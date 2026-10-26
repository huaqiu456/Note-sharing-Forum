package com.huaqiu.xiaohashu.user.biz.mapper;

import com.huaqiu.xiaohashu.user.biz.domain.po.User;

public interface UserMapper {
    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);
}