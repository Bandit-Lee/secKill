package com.bandit.seckill.service;

import com.bandit.seckill.entity.TGoods;
import com.bandit.seckill.vo.GoodsVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ITGoodsService extends IService<TGoods> {

    /**
     * 返回商品列表
     **/
    List<GoodsVo> findGoodsVo();

    /**
     * 获取商品详情
     **/
    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}
