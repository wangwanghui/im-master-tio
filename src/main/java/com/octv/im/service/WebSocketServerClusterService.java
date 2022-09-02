package com.octv.im.service;

import com.octv.im.entity.ChatMessageBean;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

public interface WebSocketServerClusterService {
    Boolean sendToUser(ChatMessageBean chatMessageBean);

    Boolean sendToGroup(ChatMessageBean chatMessageBean);

    void messageClusterSubscribe(Message message, Channel channel);
}
