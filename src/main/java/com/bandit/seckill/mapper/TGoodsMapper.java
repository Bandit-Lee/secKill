package com.bandit.seckill.mapper;



import com.bandit.seckill.entity.TGoods;
import com.bandit.seckill.vo.GoodsVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * 商品表 Mapper 接口
 */
@Mapper
public interface TGoodsMapper extends BaseMapper<TGoods> {

    /**
     * 返回商品列表
     **/
    List<GoodsVo> findGoodsVo();

    GoodsVo findGoodsVoByGoodsId(Long goodsId);
}
