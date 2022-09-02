package com.octv.im.service.impl;

import static com.octv.im.constant.RabbitKeyConstant.*;
import static com.octv.im.constant.RedisKeyConst.OFFLINE_CHAT_PERSON_KEY_PREFIX;
import static com.octv.im.util.DateUtils.YY_MM_DD_HH_MM_SS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octv.im.config.ChatServerProperties;
import com.octv.im.constant.ChatMessageConstant;
import com.octv.im.constant.SocketServerConstant;
import com.octv.im.entity.ChatMessageBean;
import com.octv.im.service.RabbitProductService;
import com.octv.im.service.IMServerService;
import com.octv.im.service.WebSocketServerClusterService;
import com.octv.im.service.RedisClusterService;
import com.octv.im.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.websocket.common.WsResponse;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IMServerServiceImpl implements IMServerService {
    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;
    @Autowired
    private RabbitProductService rabbitProductService;
    @Autowired
    private ChatServerProperties chatServerProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisClusterService redisClusterService;

    @Autowired
    private WebSocketServerClusterService webRabbitClusterService;

    @Override
    public void chatMessageDispatch(String msg, ChannelContext channelContext) {
        try {
            log.info("receive message : {} ", msg);
            ChatMessageBean chatMessageBean = objectMapper.readValue(msg, ChatMessageBean.class);
            // chatMessageBean.setType(TEXT_TYPE_MESSAGE); 用于后期区分图片或者文本
            this.messageHandle(channelContext, chatMessageBean);
        } catch (Exception e) {
            log.error("chatMessageDispatch error , msg >> {} ", msg, e);
        }
    }

    @Override
    public void chatMessageDispatch(ChatMessageBean chatMessageBean, ChannelContext channelContext) {
        try {
            log.info("receive message : {} ", chatMessageBean);
            this.messageHandle(channelContext, chatMessageBean);
        } catch (Exception e) {
            log.error("chatMessageDispatch error , msg >> {} ", chatMessageBean, e);
        }
    }

    @Override
    public Set<ZSetOperations.TypedTuple<Serializable>> listOfflineMessages(ChannelContext channelContext) {
        String key = StringUtils.joinWith(".", OFFLINE_CHAT_PERSON_KEY_PREFIX, channelContext.userid);
        return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 1,
                chatServerProperties.getOffLineMessageLimit(), 0, chatServerProperties.getOffLineMessageLimit());
    }

    @Override
    public int deleteOfflineMessage(ChannelContext channelContext, Set<ZSetOperations.TypedTuple<Serializable>> set) {
        String key = StringUtils.joinWith(".", OFFLINE_CHAT_PERSON_KEY_PREFIX, channelContext.userid);
        List<Double> scores = set.stream().map(ZSetOperations.TypedTuple::getScore).collect(Collectors.toList());
        double minScore = scores.stream().min(Double::compareTo).get();
        double maxScore = scores.stream().max(Double::compareTo).get();
        redisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
        return 0;
    }

    private void messageHandle(ChannelContext channelContext, ChatMessageBean chatMessageBean) {
        try {
            if (chatMessageBean.getCode() == ChatMessageConstant.PRIVATE_CHAT_CODE
                    || chatMessageBean.getCode() == ChatMessageConstant.GROUP_CHAT_CODE) {
                chatMessageBean.setStatus(chatMessageBean.getStatus() != null ? chatMessageBean.getStatus() : 0); // 为0未读
                chatMessageBean.setSendTime(DateUtils.getCurrentDate("yyyy-MM-dd HH:mm:ss"));
                String msgID = UUID.randomUUID().toString().replace("-", "");
                chatMessageBean.setMsgId(msgID);
            }
            WsResponse wsResponse = WsResponse.fromText(objectMapper.writeValueAsString(chatMessageBean),
                    SocketServerConstant.CHARSET);
            switch (chatMessageBean.getCode()) {
            // 私聊
            case ChatMessageConstant.PRIVATE_CHAT_CODE:
                String receiverUserId = chatMessageBean.getReceiverUserId();
                // 自己要收到一份
                if (!StringUtils.endsWithIgnoreCase(chatMessageBean.getReceiverUserId(),
                        chatMessageBean.getSendUserId())) {
                    Tio.send(channelContext, wsResponse);
                }
                ChannelContext receiverUserChannelContext = Tio.getByBsId(channelContext.tioConfig, receiverUserId);
                if (receiverUserChannelContext == null) {
                    String key = StringUtils.joinWith(".", OFFLINE_CHAT_PERSON_KEY_PREFIX, receiverUserId);
                    // 取出当前最大score
                    Set<ZSetOperations.TypedTuple<Serializable>> set = redisTemplate.opsForZSet()
                            .reverseRangeWithScores(key, 0, 0);
                    double maxScore = set != null && !set.isEmpty() ? set.stream().findFirst().get().getScore() : 0;
                    log.info("接收者不在当前服务器或离线：{} ", receiverUserId);
                    redisTemplate.opsForZSet().add(key, chatMessageBean, maxScore + 1);
                    // 当前用户不在当前服务器上又开启了集群模式
                    if (chatServerProperties.getOpenCluster()) {
                        // webSocketClusterService.sendToUser(chatMessageBean);
                        webRabbitClusterService.sendToUser(chatMessageBean);
                    }
                } else {
                    Tio.send(receiverUserChannelContext, wsResponse);
                }
                rabbitProductService.sendMessage(PERSON_CHAT_EXCHANGE, PERSON_CHAT_ROUT_KEY, chatMessageBean);
                break;
            // 群聊
            case ChatMessageConstant.GROUP_CHAT_CODE:
                // 开启集群模式
                if (chatServerProperties.getOpenCluster()) {
                    // webSocketClusterService.sendToGroup(chatMessageBean);
                    webRabbitClusterService.sendToGroup(chatMessageBean);
                } else {
                    if (!Tio.isInGroup(chatMessageBean.getGroupId(), channelContext)) {
                        Tio.bindGroup(channelContext, chatMessageBean.getGroupId());
                    }
                    Tio.sendToGroup(channelContext.tioConfig, chatMessageBean.getGroupId(), wsResponse);
                }
                rabbitProductService.sendMessage(GROUP_CHAT_EXCHANGE, GROUP_CHAT_ROUT_KEY, chatMessageBean);
                break;
            // 系统消息
            case ChatMessageConstant.SYSTEM_MESSAGE_CODE:
                // @TODO 需要广播的系统消息
                break;
            // 退出群聊
            case ChatMessageConstant.USER_GROUP_ROOM_ACTION_CODE:
                // {"userID":"wang","groupAction":"1",groupID:"10","sendTime":"2022-05-05 10:20:58"}
                if (StringUtils.endsWithIgnoreCase(chatMessageBean.getGroupAction(),
                        ChatMessageConstant.LEAVE_CHAT_GROUP_ROOM)) {
                    Tio.unbindGroup(chatMessageBean.getGroupId(), channelContext);
                }
                break;
            // 消息返回确认
            case ChatMessageConstant.MESSAGE_STATUS_RETURN_CODE:
                chatMessageBean.setReadTime(DateUtils.getCurrentDate(YY_MM_DD_HH_MM_SS));
                rabbitProductService.sendMessage(MESSAGE_STATUS_EXCHANGE, MESSAGE_STATUS_ROUT_KEY, chatMessageBean);
                break;
            default:
            }
        } catch (Exception e) {
            log.error("message forward error:", e);
            e.printStackTrace();
        }
    }
}
