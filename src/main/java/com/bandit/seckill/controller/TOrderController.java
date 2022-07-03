package com.bandit.seckill.controller;


import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.service.ITOrderService;
import com.bandit.seckill.vo.OrderDetailVo;
import com.bandit.seckill.vo.RespBean;
import com.bandit.seckill.vo.RespBeanEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 前端控制器
 */
@RestController
@RequestMapping("/order")
@Api(value = "订单", tags = "订单")
public class TOrderController {

    @Autowired
    private ITOrderService itOrderService;

    @ApiOperation("订单")
    @GetMapping("/detail")
    @ResponseBody
    public RespBean detail(TUser tUser, Long orderId) {
        if (tUser == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        OrderDetailVo orderDetailVo = itOrderService.detail(orderId);
        return RespBean.success(orderDetailVo);
    }
}
