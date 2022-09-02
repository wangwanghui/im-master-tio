package com.octv.im.service.impl;

import static com.octv.im.constant.RedisKeyConst.OFFLINE_CHAT_PERSON_KEY_PREFIX;
import com.octv.im.config.ChatServerProperties;
import com.octv.im.constant.SocketServerConstant;
import com.octv.im.entity.ChatMessageBean;
import com.octv.im.service.RedisClusterService;
import com.octv.im.util.JacksonUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.utils.lock.ReadLockHandler;
import org.tio.utils.lock.SetWithLock;
import org.tio.websocket.common.WsResponse;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executor;

@Service
public class RedisClusterServiceImpl implements RedisClusterService, InitializingBean {
    private static final String topic_patten = "chat";
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ChatServerProperties chatServerProperties;
    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    @Autowired
    private Executor executor;

    @Override
    public Boolean sendToUser(ChatMessageBean chatMessageBean) {
        RTopic topic = redissonClient.getTopic(topic_patten);
        topic.publish(chatMessageBean);
        return true;
    }

    @Override
    public Boolean sendToGroup(ChatMessageBean chatMessageBean) {
        RTopic topic = redissonClient.getTopic(topic_patten);
        topic.publish(chatMessageBean);
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 开启集群部署多个服务
        if (chatServerProperties.getOpenCluster()) {
            // 开启订阅
            this.subscribeMsg();
        }
    }

    // 通过redission的发布订阅机制实现集群模式
    private void subscribeMsg() {
        RTopic topic = redissonClient.getTopic(topic_patten);
        topic.addListenerAsync(ChatMessageBean.class, new MessageListener<ChatMessageBean>() {
            @Override
            public void onMessage(CharSequence charSequence, ChatMessageBean chatMessageBean) {
                // 单聊信息
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
                                    }
                                }
                            }
                        });
                    }
                } else {
                    // 集群中发送群聊消息
                    WsResponse wsResponse = WsResponse.fromText(JacksonUtil.writeValueAsString(chatMessageBean),
                            WsResponse.CHARSET_NAME);
                    Tio.sendToGroup(SocketServerConstant.tioServerConfig, chatMessageBean.getGroupId(), wsResponse);
                }
            }
        });
    }

}
