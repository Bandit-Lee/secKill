package com.bandit.seckill.vo;

import com.bandit.seckill.entity.TOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailVo {

    private TOrder tOrder;

    private GoodsVo goodsVo;
}
