package com.bandit.seckill.rabbitmq;

import com.bandit.seckill.entity.SecKillMessage;
import com.bandit.seckill.entity.TSeckillOrder;
import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.service.ITGoodsService;
import com.bandit.seckill.service.ITOrderService;
import com.bandit.seckill.utils.JsonUtil;
import com.bandit.seckill.vo.GoodsVo;
import com.bandit.seckill.vo.RespBean;
import com.bandit.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

/**
 * @author Bandit
 * @createTime 2022/7/2 19:04
 */
@Service
@Slf4j
public class MQReceiver {

//    @RabbitListener(queues = "secKill.queue")
//    public void receive(Object msg) {
//        log.info("MQReceiver receive: {}", msg);
//    }

    @Autowired
    private ITGoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ITOrderService orderService;


    @RabbitListener(queues = "secKillQueue")
    public void receive(String msg) {
        log.info("secKillMessage received: {}", msg);
        SecKillMessage secKillMessage = JsonUtil.jsonStr2Object(msg, SecKillMessage.class);
        Long goodsId = secKillMessage.getGoodsId();
        TUser user = secKillMessage.getTUser();
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            return;
        }
        // 判断是否重复抢购从缓存中获取是否有订单 解决超卖
        TSeckillOrder seckillOrder = (TSeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
            // 重复抢购，限购一件
            return;
        }
        // 下单
        orderService.secKill(user, goodsVo);

    }

}
