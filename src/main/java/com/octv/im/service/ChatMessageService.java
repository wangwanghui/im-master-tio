package com.octv.im.service;

import com.octv.im.entity.*;

import java.util.List;

public interface ChatMessageService {
    List<ChatHistoryBean> listChatHistories(ChatHistoryConditionBean chatHistoryConditionBean);

    String uploadChatImages(ChatMessageBean chatMessageBean);

    int updateMessageStatus();

    StatusBean sendMessage(UserMessageForwardBean messageForwardBean);

}
