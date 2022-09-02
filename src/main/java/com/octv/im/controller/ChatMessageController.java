package com.octv.im.controller;

import com.octv.im.entity.*;
import com.octv.im.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息API
 *
 * @Author
 * @Description
 */
@RestController
@RequestMapping("/chat")
public class ChatMessageController {
    @Autowired
    private ChatMessageService chatMessageService;

    // 查询聊天记录
    @GetMapping(value = "/history")
    public List<ChatHistoryBean> listGroupMessageHistories(ChatHistoryConditionBean chatHistoryConditionBean) {
        return chatMessageService.listChatHistories(chatHistoryConditionBean);
    }

    // 发送聊天图片
    @PostMapping(value = "/upload_images")
    public String uploadImages(ChatMessageBean chatMessageBean) {
        return chatMessageService.uploadChatImages(chatMessageBean);
    }

    @PostMapping(value = "/sendMsg")
    public StatusBean sendMsg(@RequestBody UserMessageForwardBean messageForwardBean) {
        return chatMessageService.sendMessage(messageForwardBean);
    }



}
