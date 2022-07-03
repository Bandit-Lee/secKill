package com.bandit.seckill.mapper;


import com.bandit.seckill.entity.TSeckillOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀订单表 Mapper 接口
 */
@Mapper
public interface TSeckillOrderMapper extends BaseMapper<TSeckillOrder> {

}
