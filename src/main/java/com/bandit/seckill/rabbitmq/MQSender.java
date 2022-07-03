package com.bandit.seckill.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Bandit
 * @createTime 2022/7/2 19:03
 */
@Slf4j
@Service
public class MQSender {

    @Resource
    private RabbitTemplate rabbitTemplate;

//    public void send(Object msg) {
//        log.info("MQSender send msg: {}", msg);
//        rabbitTemplate.convertAndSend("secKill.queue",msg);
//    }

    public void sendKillMessage(String message) {
        log.info("发送消息：" + message);
        rabbitTemplate.convertAndSend("secKillExchange", "secKill.message", message);
    }
}
