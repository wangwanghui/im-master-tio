package com.octv.im.service.impl;

import static com.octv.im.constant.ChatMessageConstant.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octv.im.config.ChatServerProperties;
import com.octv.im.constant.SocketServerConstant;
import com.octv.im.entity.*;
import com.octv.im.service.ChatMessageService;
import com.octv.im.service.IMServerService;
import com.octv.im.util.DateUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.websocket.common.WsResponse;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ChatServerProperties chatServerProperties;
    @Autowired
    private IMServerService IMServerService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ChatHistoryBean> listChatHistories(ChatHistoryConditionBean chatHistoryConditionBean) {
        Query query = new Query();
        List<ChatHistoryBean> chatHistoryBeans = new ArrayList<>();
        // 分页
        if (chatHistoryConditionBean.getOffset() != null || chatHistoryConditionBean.getLimit() != null) {
            int page = chatHistoryConditionBean.getOffset() != null ? chatHistoryConditionBean.getOffset() : 1;
            int limit = chatHistoryConditionBean.getLimit() != null ? chatHistoryConditionBean.getLimit()
                    : Integer.MAX_VALUE;
            query.skip((long) (page - 1) * limit);
            query.limit(limit);
        }
        // 时间段
        if (chatHistoryConditionBean.getFrom() != null && chatHistoryConditionBean.getTo() != null) {
            // mongoDB 查询当天的需要特殊处理
            LocalDateTime from = LocalDateTime.parse(chatHistoryConditionBean.getFrom() + "T00:00:00");
            LocalDateTime to = LocalDateTime.parse(chatHistoryConditionBean.getTo() + "T23:59:59");
            query.addCriteria(Criteria.where("send_time").gte(from).lte(to));
        }
        List<ChatMessageBean> chatMessageBeans = null;
        try {
            if ("Y".equalsIgnoreCase(chatHistoryConditionBean.getPersonalMsg())) {
                // 查询单聊消息
                chatMessageBeans = mongoTemplate.find(
                        query.addCriteria(Criteria.where("send_user_id").is(chatHistoryConditionBean.getSend())
                                .and("receive_user_id").is(chatHistoryConditionBean.getReceive())),
                        ChatMessageBean.class, PERSON_CHAT_HISTORY_COLLECTION);
            } else {
                // 查询群聊消息
                chatMessageBeans = mongoTemplate.find(
                        query.addCriteria(Criteria.where("group_id").is(chatHistoryConditionBean.getGroupID())),
                        ChatMessageBean.class, GROUP_CHAT_HISTORY_COLLECTION);
            }
            for (ChatMessageBean chatMessageBean : chatMessageBeans) {
                ChatHistoryBean chatHistoryBean = constructChatHistoryDTO(chatMessageBean);
                chatHistoryBeans.add(chatHistoryBean);
            }
        } catch (Exception e) {
            log.error("listChatHistories error .", e);
        }
        return chatHistoryBeans;
    }

    @Override
    public String uploadChatImages(ChatMessageBean chatMessageBean) {
        MultipartFile file = chatMessageBean.getFile();
        assetImageLegal(file);
        String uploadPath = null;
        try {
            // 需要为每张图片给个编号方便识别
            String fileName = getExtensionFileName(chatMessageBean);
            uploadPath = chatServerProperties.getUploadImagesPath().endsWith("/")
                    ? chatServerProperties.getUploadImagesPath() + fileName
                    : chatServerProperties.getUploadImagesPath() + "/" + fileName;
            @Cleanup
            FileOutputStream fileOutputStream = new FileOutputStream(uploadPath);
            IOUtils.copy(file.getInputStream(), fileOutputStream);
            chatMessageBean.setCode(chatMessageBean.getGroupId() != null ? GROUP_CHAT_CODE : PRIVATE_CHAT_CODE);
            chatMessageBean.setType(IMAGES_TYPE_MESSAGE);
            chatMessageBean.setFilePath(uploadPath);
            if (SocketServerConstant.tioServerConfig != null) {
                ChannelContext channelContext = Tio.getByBsId(SocketServerConstant.tioServerConfig,
                        chatMessageBean.getSendUserId());
                chatMessageBean.setFile(null);
                IMServerService.chatMessageDispatch(chatMessageBean, channelContext);
            } else {
                // chat server already down
                throw new RuntimeException("chat server fail please check.");
            }
        } catch (Exception e) {
            // 失败需要删除该文件
            log.error("uploadChatImages fail .", e);
        }
        return uploadPath;
    }

    @Override
    public int updateMessageStatus() {
        return 0;
    }

    @Override
    public StatusBean sendMessage(UserMessageForwardBean userMessageForwardBean) {
        StatusBean statusBean = new StatusBean();
        String status = null;
        int code = 200;
        try {
            if (SocketServerConstant.tioServerConfig != null) {
                ChannelContext channelContext = Tio.getByBsId(SocketServerConstant.tioServerConfig,
                        userMessageForwardBean.getUserId());
                ChatMessageBean chatMessageBean = new ChatMessageBean();
                chatMessageBean.setCode(1);
                chatMessageBean.setType("3");
                chatMessageBean.setSendUserId("system");
                chatMessageBean.setSendTime(DateUtils.getCurrentDate());
                chatMessageBean.setReceiverUserId(userMessageForwardBean.getUserId());
                chatMessageBean.setMsg("个人消息");
                chatMessageBean.setExt(objectMapper.convertValue(userMessageForwardBean,Map.class));
                log.info("user send message: {} ", objectMapper.writeValueAsString(chatMessageBean));
                WsResponse wsResponse = WsResponse.fromText(objectMapper.writeValueAsString(chatMessageBean),
                        SocketServerConstant.CHARSET);
                Tio.send(channelContext, wsResponse);
            } else {
                // chat server already down
                throw new RuntimeException("chat server fail please check.");
            }
        } catch (Exception e) {
            status = "Tio server fail";
            code = 500;
            log.error("sendMessage  error {}", userMessageForwardBean, e);
        }
        statusBean.setStatus(status != null ? status : "success");
        statusBean.setCode(code);
        return statusBean;
    }

    private ChatHistoryBean constructChatHistoryDTO(ChatMessageBean chatMessageBean) {
        ChatHistoryBean chatHistoryBean = new ChatHistoryBean();
        chatHistoryBean.setType(chatHistoryBean.getGroupID() != null ? "group chat" : "private chat");
        chatHistoryBean.setMsgId(chatMessageBean.getMsgId());
        chatHistoryBean.setSendTime(chatMessageBean.getSendTime());
        chatHistoryBean.setReceiverUserId(chatMessageBean.getReceiverUserId());
        chatHistoryBean.setSendUserId(chatMessageBean.getSendUserId());
        chatHistoryBean.setRead(chatMessageBean.getStatus() == 1);
        chatHistoryBean.setMsg(chatMessageBean.getMsg());
        chatHistoryBean.setGroupID(chatMessageBean.getGroupId());
        chatHistoryBean.setReadTime(chatMessageBean.getReadTime());
        return chatHistoryBean;
    }

    private void assetImageLegal(MultipartFile file) {
        String suffix = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
        if (Arrays.stream(chatServerProperties.getUploadImagesType()).noneMatch(value -> value.contains(suffix))) {
            throw new RuntimeException("suffix is not support");
        }
    }

    private String getExtensionFileName(ChatMessageBean chatMessageBean) {
        String suffix = StringUtils.substringAfter(chatMessageBean.getFile().getOriginalFilename(), ".");
        return chatMessageBean.getSendUserId() + "_to_" + chatMessageBean.getReceiverUserId() + "_"
                + System.currentTimeMillis() + "." + suffix;
    }
}
