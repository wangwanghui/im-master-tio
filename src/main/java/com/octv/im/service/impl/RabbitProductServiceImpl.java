package com.octv.im.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octv.im.service.RabbitProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import javax.annotation.PostConstruct;

@Service
@Slf4j
public class RabbitProductServiceImpl implements RabbitProductService {

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
        // @TODO
    }

    // 消息未到交换机处理
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("message send to exchange error . message >> {} , exchange >> {} , routKey >> {} ",
                returnedMessage.getMessage(), returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        // @TODO
    }

    @Override
    public void sendMessage(String exchange, String routingKey, Object message, Long expireTime) {
        Assert.notNull(message, "message can't be NULL");
        Assert.notNull(exchange, "exchange can't be NULL");
        // Assert.notNull(routingKey, "routingKey can't be NULL");
        this.convertAndSend(exchange, routingKey, message, expireTime);
    }

    @Override
    public void sendMessage(String exchange, String routingKey, Object message) {
        Assert.notNull(message, "message can't be NULL");
        Assert.notNull(exchange, "exchange can't be NULL");
        // Assert.notNull(routingKey, "routingKey can't be NULL");
        this.convertAndSend(exchange, routingKey, message, null);
    }

    private void convertAndSend(String exchange, String routingKey, Object message, Long expireTime) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
        } catch (Exception e) {
            log.error("RabbitMQ send message error, body:{}, exchange:{}, routingKey:{}", message, exchange, routingKey,
                    e);
            // TODO
        }
    }
}
