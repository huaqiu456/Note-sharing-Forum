package com.huaqiu.xiaohashu.note.mapper;

import com.huaqiu.xiaohashu.note.domain.po.Channel;

public interface ChannelMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Channel record);

    int insertSelective(Channel record);

    Channel selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Channel record);

    int updateByPrimaryKey(Channel record);
}