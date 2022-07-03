package com.bandit.seckill.controller;


import com.bandit.seckill.entity.SecKillMessage;
import com.bandit.seckill.entity.TOrder;
import com.bandit.seckill.entity.TSeckillOrder;
import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.exception.GlobalException;
import com.bandit.seckill.rabbitmq.MQSender;
import com.bandit.seckill.service.impl.TGoodsServiceImpl;
import com.bandit.seckill.service.impl.TOrderServiceImpl;
import com.bandit.seckill.service.impl.TSeckillOrderServiceImpl;
import com.bandit.seckill.utils.JsonUtil;
import com.bandit.seckill.vo.GoodsVo;
import com.bandit.seckill.vo.RespBean;
import com.bandit.seckill.vo.RespBeanEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
@Api(value = "秒杀", tags = "秒杀")
public class SeKillController implements InitializingBean {

    @Autowired
    TGoodsServiceImpl goodsService;

    @Autowired
    TOrderServiceImpl orderService;

    @Autowired
    TSeckillOrderServiceImpl seckillOrderService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MQSender mqSender;

    @Autowired
    private RedisScript<Long> redisScript;

    private Map<Long, Boolean> EmptyStockMap = new ConcurrentHashMap<>();


    @ApiOperation("获取验证码")
    @GetMapping(value = "/captcha")
    public void verifyCode(TUser tUser, Long goodsId, HttpServletResponse response) {
        if (tUser == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        //设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + tUser.getId() + ":" + goodsId, captcha.text(), 300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("验证码生成失败:{}", e.getMessage());
        }
    }


    @ApiOperation("获取秒杀地址")
    @GetMapping(value = "/path")
    @ResponseBody
    public RespBean getPath(TUser tuser, Long goodsId, String captcha, HttpServletRequest request) {
        if (tuser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        //限制访问次数，5秒内访问5次
//        String uri = request.getRequestURI();
//        captcha = "0";
//        Integer count = (Integer) valueOperations.get(uri + ":" + tuser.getId());
//        if (count == null) {
//            valueOperations.set(uri + ":" + tuser.getId(), 1, 5, TimeUnit.SECONDS);
//        } else if (count < 5) {
//            valueOperations.increment(uri + ":" + tuser.getId());
//        } else {
//            return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REACHED);
//        }
        boolean check = orderService.checkCaptcha(tuser, goodsId, captcha);
        if (!check) {
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(tuser, goodsId);
        return RespBean.success(str);
    }



    /**
     * ===================== 未优化前================ <br>
     * Windows 1000线程 * 10 QPS：30.3 限制50个 卖出497个  5000 * 1 QPS: 28.7 <br>
     * ===================== 订单缓存优化/SQL加唯一索引后 ============== <br>
     * Windows 1000线程 * 10 QPS：895   (超卖解决)   5000 * 1 QPS: 400 <br>
     * ===================== redis预减库存、本地缓存减少redis通信，消息队列处理 订单
     * Windows 1000线程 * 10 QPS：3100  (超卖解决)   5000 * 1 QPS: 1000 <br>
     *
     * @return
     */
    @ApiOperation("秒杀功能")
    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill(@PathVariable String path, TUser user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        // 本地缓存，减少与redis的通信
        if (EmptyStockMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 判断请求的path是否正确
        boolean check = orderService.checkPath(user, goodsId, path);
        if (!check) {
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }
        // 判断是否重复抢购从缓存中获取是否有订单 解决超卖
        TSeckillOrder seckillOrder = (TSeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
            // 重复抢购，限购一件
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        // redis 预减库存 原子递减 decrement
        Long stock = valueOperations.decrement("secKillGoods:" + goodsId);
        // redis 预减库存 ---》 改为lua脚本
        //Long stock = (Long) redisTemplate.execute(redisScript, Collections.singletonList("secKillGoods:" + goodsId), Collections.EMPTY_LIST);
        if (stock < 0) {
            EmptyStockMap.put(goodsId, true);
            valueOperations.increment("secKillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }


        SecKillMessage secKillMessage = new SecKillMessage(user, goodsId);
        // 发消息
        mqSender.sendKillMessage(JsonUtil.object2JsonStr(secKillMessage));
        return RespBean.success(0);

        /*=========之前的逻辑, 第一次放进缓存的是订单，解决超卖，并发问题完全是靠 SQL事务的行锁, 这样肯定会崩==========
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        // 判断库存
        if (goodsVo.getStockCount() < 1) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        // 判断是否重复抢购从缓存中获取是否有订单 解决超卖
        TSeckillOrder seckillOrder = (TSeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
            // 重复抢购，限购一件
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        // 秒杀
        TOrder tOrder = orderService.secKill(user, goodsVo);
        return tOrder != null ? RespBean.success(tOrder) : RespBean.error(RespBeanEnum.REPEATE_ERROR);
        */
    }


    /**
     * 初始化操作，把商品库存数量放进 redis
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        // key:秒杀商品ID，value:商品的数量
        list.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("secKillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            EmptyStockMap.put(goodsVo.getId(), false);
        });
    }


    /**
     * 获取秒杀结果
     *
     * @param tUser
     * @param goodsId
     * @return orderId 成功 ；-1 秒杀失败 ；0 排队中
     **/
    @ApiOperation("获取秒杀结果")
    @GetMapping("getResult")
    @ResponseBody
    public RespBean getResult(TUser tUser, Long goodsId) {
        if (tUser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = seckillOrderService.getResult(tUser, goodsId);
        return RespBean.success(orderId);
    }





    // ======================================废弃===============================================
    @ApiOperation("秒杀功能-废弃")
    @RequestMapping(value = "/doSeckill2", method = RequestMethod.POST)
    public String doSecKill2(Model model, TUser user, Long goodsId) {
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goodsVo.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //判断是否重复抢购
        TSeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<TSeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }
        TOrder tOrder = orderService.secKill(user, goodsVo);
        model.addAttribute("order", tOrder);
        model.addAttribute("goods", goodsVo);
        return "orderDetail";
    }




}
