package com.octv.im.service.impl;

import static com.octv.im.constant.RedisKeyConst.OFFLINE_CHAT_PERSON_KEY_PREFIX;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octv.im.constant.SocketServerConstant;
import com.octv.im.entity.ChatMessageBean;
import com.octv.im.service.RabbitProductService;
import com.octv.im.service.WebSocketServerClusterService;
import com.octv.im.util.JacksonUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.utils.lock.ReadLockHandler;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsResponse;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@Slf4j
public class WebSocketClusterServiceImpl implements WebSocketServerClusterService {

    public static final String FANOUT_EXCHANGE = "fanout_exchange";

    // 发布订阅模式集群下每台机子需要对应一个队列名
    public static final String FANOUT_QUEUE = "fanout_queue";

    public static final String ROUTE_KEY = null;

    @Autowired
    private RabbitProductService rabbitProductService;

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean sendToUser(ChatMessageBean chatMessageBean) {
        rabbitProductService.sendMessage(FANOUT_EXCHANGE, ROUTE_KEY, chatMessageBean);
        return null;
    }

    @Override
    public Boolean sendToGroup(ChatMessageBean chatMessageBean) {
        System.out.println("集群A这个时候发出去的 ---》" + LocalDateTime.now());
        rabbitProductService.sendMessage(FANOUT_EXCHANGE, ROUTE_KEY, chatMessageBean);
        return null;
    }

    // 集群下需要设置消息过期时间防止用户并不在任何一台服务器上导致离线消息和队列消息重复 默认 10秒
    @RabbitListener(containerFactory = "simpleRabbitListenerContainerFactory", bindings =
    @QueueBinding(value = @Queue(value = FANOUT_QUEUE, autoDelete = "false", durable = "true",arguments = {
            @Argument(name = "x-message-ttl", value = "6000",type = "java.lang.Integer") }), exchange =
    @Exchange(value = FANOUT_EXCHANGE, type = ExchangeTypes.FANOUT)))
    @Override
    public void messageClusterSubscribe(Message message, Channel channel) {
        try {
            System.out.println("messageSubscribeConsume 在这个时候 ---》收到消息" + LocalDateTime.now());
            String msg = new String(message.getBody(), "UTF-8");
            ChatMessageBean chatMessageBean = objectMapper.readValue(msg, ChatMessageBean.class);
            if (chatMessageBean != null) {
                boolean singleChat = StringUtils.isEmpty(chatMessageBean.getGroupId());
                if (singleChat) {
                    SetWithLock<ChannelContext> setWithLock = Tio.getByUserid(SocketServerConstant.tioServerConfig,
                            singleChat ? chatMessageBean.getReceiverUserId() : chatMessageBean.getSendUserId());
                    if (setWithLock != null) {
                        System.out.println(
                                JacksonUtil.writeValueAsString(chatMessageBean) + "time ---》" + LocalDateTime.now());
                        WsResponse wsResponse = WsResponse.fromText(JacksonUtil.writeValueAsString(chatMessageBean),
                                WsResponse.CHARSET_NAME);
                        setWithLock.handle(new ReadLockHandler<Set<ChannelContext>>() {
                            @Override
                            public void handler(Set<ChannelContext> set) {
                                for (ChannelContext channelContext : set) {
                                    // 在不同服务器的单聊
                                    boolean success = Tio.send(channelContext, wsResponse);
                                    if (success) {
                                        String key = StringUtils.joinWith(".", OFFLINE_CHAT_PERSON_KEY_PREFIX,
                                                channelContext.userid);
                                        redisTemplate.opsForZSet().remove(key, chatMessageBean);
                                        try {
                                            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                        });
                    }
                } else {
                    // 集群中发送群聊消息需要等待发送结果来ACK,防止重复ACK
                    WsResponse wsResponse = WsResponse.fromText(JacksonUtil.writeValueAsString(chatMessageBean),
                            WsResponse.CHARSET_NAME);
                    boolean success = Tio.bSendToGroup(SocketServerConstant.tioServerConfig,
                            chatMessageBean.getGroupId(), wsResponse);
                    if (success) {
                        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    }
                }
            }
        } catch (Exception e) {
            log.error("messageSubscribeConsume fail exchange {}, queue {}  message {}", FANOUT_EXCHANGE, FANOUT_QUEUE,
                    String.valueOf(message.getBody()), e);
        }
    }
}
