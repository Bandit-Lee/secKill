package com.bandit.seckill.vo;

import com.bandit.seckill.entity.TUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品详情返回对象
 *
 * @author: LC
 * @date 2022/3/6 10:06 上午
 * @ClassName: DetailVo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailVo {


    private TUser tUser;

    private GoodsVo goodsVo;

    private int secKillStatus;

    private int remainSeconds;


}
