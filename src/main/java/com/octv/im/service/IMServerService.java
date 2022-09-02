package com.octv.im.service;

import com.octv.im.entity.ChatMessageBean;
import org.springframework.data.redis.core.ZSetOperations;
import org.tio.core.ChannelContext;

import java.io.Serializable;
import java.util.Set;

public interface IMServerService {
    void chatMessageDispatch(String msg, ChannelContext channelContext);

    void chatMessageDispatch(ChatMessageBean chatMessageBean, ChannelContext channelContext);

    Set<ZSetOperations.TypedTuple<Serializable>> listOfflineMessages(ChannelContext channelContext);

    int deleteOfflineMessage(ChannelContext channelContext, Set<ZSetOperations.TypedTuple<Serializable>> set);
}
