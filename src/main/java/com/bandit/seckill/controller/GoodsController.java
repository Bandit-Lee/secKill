package com.bandit.seckill.controller;

import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.service.ITGoodsService;
import com.bandit.seckill.service.ITUserService;
import com.bandit.seckill.vo.DetailVo;
import com.bandit.seckill.vo.GoodsVo;
import com.bandit.seckill.vo.RespBean;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("goods")
@Api(value = "商品", tags = "商品")
@Slf4j
public class GoodsController {

    @Autowired
    private ITUserService itUserService;

    @Autowired
    private ITGoodsService itGoodsService;

    @Resource
    private ThymeleafViewResolver thymeleafViewResolver;
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * ============== 优化前 =================================
     * windows   : 1000线程 * 10 QPS：1000 5000 * 1 QPS：800
     * linux1核2g: 1000线程 * 10 崩了       5000 * 1 QPS：50
     * =========================================================
     *
     * ============== 第一次优化页面缓存 =========================
     * windows  : 1000线程 * 10 QPS：2790 5000 * 1 QPS：1600 QPS直接翻了两倍
     *
     */
    @ApiOperation("商品列表")
    @GetMapping(value = "/toList", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, TUser user, HttpServletRequest request, HttpServletResponse response) {
        // 缓存存html页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsListHtml");
        if (!StringUtils.isEmpty(html)) {
            // 如果有页面直接返回
            return html;
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", itGoodsService.findGoodsVo());

        // 如果页面为空，手动渲染
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsListHtml", html, 60, TimeUnit.SECONDS);
        }
        return html;
    }


    @ApiOperation("商品详情")
    @GetMapping(value = "/detailpre/{goodsId}")
    @ResponseBody
    public RespBean toDetail(TUser user, Model model,@PathVariable Long goodsId) {
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
//        if (!StringUtils.isEmpty(html)) {
//            return html;
//        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = itGoodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsVo);
        model.addAttribute("seckillStatus", seckillStatus);

//        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
//        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
//        if (!StringUtils.isEmpty(html)) {
//            valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
//        }
        DetailVo detailVo = DetailVo.builder()
                .tUser(user)
                .goodsVo(goodsVo)
                .remainSeconds(remainSeconds)
                .secKillStatus(seckillStatus)
                .build();

        return RespBean.success(detailVo);
    }


    @ApiOperation("商品详情")
    @GetMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public RespBean toDetail2(TUser user,@PathVariable Long goodsId) {
        GoodsVo goodsVo = itGoodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        DetailVo detailVo = DetailVo.builder()
                .tUser(user)
                .goodsVo(goodsVo)
                .remainSeconds(remainSeconds)
                .secKillStatus(seckillStatus)
                .build();
        return RespBean.success(detailVo);
    }

}
