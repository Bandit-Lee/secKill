package com.bandit.seckill.service;


import com.bandit.seckill.entity.TSeckillOrder;
import com.bandit.seckill.entity.TUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 秒杀订单表 服务类
 */
public interface ITSeckillOrderService extends IService<TSeckillOrder> {


    Long getResult(TUser tUser, Long goodsId);
}
