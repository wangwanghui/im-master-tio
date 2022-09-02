package com.octv.im.service;

import com.octv.im.entity.ChatMessageBean;

public interface RedisClusterService {
    Boolean sendToUser(ChatMessageBean chatMessageBean);

    Boolean sendToGroup(ChatMessageBean chatMessageBean);
}
