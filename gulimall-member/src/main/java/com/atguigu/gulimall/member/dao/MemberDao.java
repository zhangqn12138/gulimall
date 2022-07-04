package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author zhangqingnan
 * @email zhangqingnan@gmail.com
 * @date 2022-02-26 17:53:57
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {

}
