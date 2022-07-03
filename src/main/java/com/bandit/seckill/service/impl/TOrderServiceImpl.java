package com.bandit.seckill.service.impl;


import com.bandit.seckill.entity.TOrder;
import com.bandit.seckill.entity.TSeckillGoods;
import com.bandit.seckill.entity.TSeckillOrder;
import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.exception.GlobalException;
import com.bandit.seckill.mapper.TOrderMapper;
import com.bandit.seckill.service.ITGoodsService;
import com.bandit.seckill.service.ITOrderService;
import com.bandit.seckill.service.ITSeckillGoodsService;
import com.bandit.seckill.service.ITSeckillOrderService;
import com.bandit.seckill.utils.MD5Util;
import com.bandit.seckill.utils.UUIDUtil;
import com.bandit.seckill.vo.GoodsVo;
import com.bandit.seckill.vo.OrderDetailVo;
import com.bandit.seckill.vo.RespBeanEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 服务实现类
 *
 * @author LiChao
 * @since 2022-03-03
 */
@Service
@Primary
public class TOrderServiceImpl extends ServiceImpl<TOrderMapper, TOrder> implements ITOrderService {

    @Autowired
    private ITSeckillGoodsService itSeckillGoodsService;

    @Autowired
    private ITGoodsService itGoodsService;

    @Autowired
    private ITSeckillOrderService itSeckillOrderService;
    @Autowired
    private TOrderMapper orderMapper;

    @Autowired
    RedisTemplate redisTemplate;


    @Transactional
    @Override
    public TOrder secKill(TUser user, GoodsVo goodsVo) {
        //减秒杀商品的库存
        TSeckillGoods seckillGoods = itSeckillGoodsService.getOne(new QueryWrapper<TSeckillGoods>().eq("goods_id", goodsVo.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
        boolean update = itSeckillGoodsService.update(new UpdateWrapper<TSeckillGoods>()
                .setSql("stock_count = stock_count - 1")
                .eq("id", seckillGoods.getId())
                .gt("stock_count", 0));
        if (seckillGoods.getStockCount() < 1) {
            redisTemplate.opsForValue().set("isStockEmpty:"+goodsVo.getId(),"0");
            return null;
        }
        //生成订单
        TOrder order = new TOrder();
        order.setUserId(user.getId());
        order.setGoodsId(goodsVo.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        TSeckillOrder tSeckillOrder = new TSeckillOrder();
        tSeckillOrder.setUserId(user.getId());
        tSeckillOrder.setOrderId(order.getId());
        tSeckillOrder.setGoodsId(goodsVo.getId());
        itSeckillOrderService.save(tSeckillOrder);
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" +goodsVo.getId(), tSeckillOrder);
        return order;
    }

    @Override
    public OrderDetailVo detail(Long orderId) {
        if (orderId == null) {
            throw new GlobalException(RespBeanEnum.ORDER_NOT_EXIST);
        }
        TOrder tOrder = orderMapper.selectById(orderId);
        GoodsVo goodsVoByGoodsId = itGoodsService.findGoodsVoByGoodsId(tOrder.getGoodsId());
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setTOrder(tOrder);
        orderDetailVo.setGoodsVo(goodsVoByGoodsId);
        return orderDetailVo;
    }

    @Override
    public String createPath(TUser user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        // 对请求的用户生成一个 随机path地址
        redisTemplate.opsForValue().set("secKillPath:" + user.getId() + ":" + goodsId, str, 1, TimeUnit.MINUTES);
        return str;
    }

    @Override
    public boolean checkPath(TUser user, Long goodsId, String path) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(path)) {
            return false;
        }
        String str = ((String) redisTemplate.opsForValue().get("secKillPath:" + user.getId() + ":" + goodsId));
        return path.equals(str);
    }

    @Override
    public boolean checkCaptcha(TUser user, Long goodsId, String captcha) {
        if (user == null || goodsId < 0 || StringUtils.isEmpty(captcha)) {
            return false;
        }
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId);
        return captcha.equals(redisCaptcha);
    }
}
