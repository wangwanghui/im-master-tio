package com.octv.im.service.impl;

import static com.octv.im.constant.ChatMessageConstant.GROUP_CHAT_HISTORY_COLLECTION;
import static com.octv.im.constant.ChatMessageConstant.PERSON_CHAT_HISTORY_COLLECTION;
import static com.octv.im.constant.RabbitKeyConstant.*;
import static com.octv.im.constant.RabbitKeyConstant.MESSAGE_STATUS_ROUT_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.octv.im.entity.ChatMessageBean;
import com.octv.im.entity.GroupChatMessageBean;
import com.octv.im.entity.PersonChatMessageBean;
import com.octv.im.service.RabbitConsumeService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class RabbitConsumeServiceImpl implements RabbitConsumeService {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MongoTemplate mongoTemplate;

    // 处理个人消息，存入数据库
    @RabbitListener(containerFactory = "simpleRabbitListenerContainerFactory", bindings = @QueueBinding(value = @Queue(value = PERSON_CHAT_QUEUE, autoDelete = "false", durable = "true"), exchange = @Exchange(value = PERSON_CHAT_EXCHANGE, type = ExchangeTypes.DIRECT), key = PERSON_CHAT_ROUT_KEY))
    @Override
    public void personalMessageConsume(Message message, Channel channel) {
        try {
            String msg = new String(message.getBody(), "UTF-8");
            PersonChatMessageBean chatMessageBean = objectMapper.readValue(msg, PersonChatMessageBean.class);
            if (chatMessageBean != null) {
                PersonChatMessageBean result = mongoTemplate.insert(chatMessageBean);
                if (result.getMsgId() != null) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            log.error("personal message consume fail exchange {}, queue {}  message {}", PERSON_CHAT_EXCHANGE,
                    PERSON_CHAT_QUEUE, String.valueOf(message.getBody()), e);
        }
    }

    // 监听群聊消息，存入数据库
    @RabbitListener(containerFactory = "simpleRabbitListenerContainerFactory", bindings = @QueueBinding(value = @Queue(value = GROUP_CHAT_QUEUE, autoDelete = "false", durable = "true"), exchange = @Exchange(value = GROUP_CHAT_EXCHANGE, type = ExchangeTypes.DIRECT), key = GROUP_CHAT_ROUT_KEY))
    @Override
    public void groupMessageConsume(Message message, Channel channel) {
        try {
            String msg = new String(message.getBody(), "UTF-8");
            GroupChatMessageBean chatMessageBean = objectMapper.readValue(msg, GroupChatMessageBean.class);
            if (chatMessageBean != null) {
                GroupChatMessageBean result = mongoTemplate.insert(chatMessageBean);
                if (result.getMsgId() != null) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            log.error("group message consume fail exchange {}, queue {}  message {}", GROUP_CHAT_EXCHANGE,
                    GROUP_CHAT_QUEUE, String.valueOf(message.getBody()), e);
        }
    }

    // 消息状态的返回，更新到数据库
    @RabbitListener(containerFactory = "simpleRabbitListenerContainerFactory", bindings = @QueueBinding(value = @Queue(value = MESSAGE_STATUS_QUEUE, autoDelete = "false", durable = "true"), exchange = @Exchange(value = MESSAGE_STATUS_EXCHANGE, type = ExchangeTypes.DIRECT), key = MESSAGE_STATUS_ROUT_KEY))
    @Override
    public void messageStatusConsume(Message message, Channel channel) {
        try {
            String msg = new String(message.getBody(), "UTF-8");
            ChatMessageBean chatMessageBean = objectMapper.readValue(msg, ChatMessageBean.class);
            if (chatMessageBean != null) {
                String collectionName = chatMessageBean.getGroupId() != null ? GROUP_CHAT_HISTORY_COLLECTION
                        : PERSON_CHAT_HISTORY_COLLECTION;
                Criteria criteria = Criteria.where("_id").is(chatMessageBean.getMsgId());
                Query query = Query.query(criteria);
                Update update = new Update();
                update.set("status", 1);
                update.set("read_time", chatMessageBean.getReadTime());
                UpdateResult result = mongoTemplate.updateFirst(query, update, ChatMessageBean.class, collectionName);
                if (result.getModifiedCount() != 0) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            log.error("message status update fail exchange {}, queue {}  message {}", GROUP_CHAT_EXCHANGE,
                    GROUP_CHAT_QUEUE, String.valueOf(message.getBody()), e);
        }
    }
}