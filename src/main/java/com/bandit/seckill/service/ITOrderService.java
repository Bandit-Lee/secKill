package com.bandit.seckill.service;

import com.bandit.seckill.entity.TOrder;
import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.vo.GoodsVo;
import com.bandit.seckill.vo.OrderDetailVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 服务类
 */
public interface ITOrderService extends IService<TOrder> {

    TOrder secKill(TUser user, GoodsVo goodsVo);

    OrderDetailVo detail(Long orderId);

    String createPath(TUser tuser, Long goodsId);

    boolean checkPath(TUser user, Long goodsId, String path);

    boolean checkCaptcha(TUser tuser, Long goodsId, String captcha);
}
