package com.bandit.seckill.service.impl;

import com.bandit.seckill.entity.TGoods;
import com.bandit.seckill.mapper.TGoodsMapper;
import com.bandit.seckill.service.ITGoodsService;
import com.bandit.seckill.vo.GoodsVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class TGoodsServiceImpl extends ServiceImpl<TGoodsMapper, TGoods> implements ITGoodsService {

    @Autowired
    private TGoodsMapper tGoodsMapper;

    @Override
    public List<GoodsVo> findGoodsVo() {
        return tGoodsMapper.findGoodsVo();
    }

    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
        return tGoodsMapper.findGoodsVoByGoodsId(goodsId);
    }
}
