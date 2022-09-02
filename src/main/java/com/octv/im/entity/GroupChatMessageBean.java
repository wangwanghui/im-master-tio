package com.octv.im.entity;


import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "group_chat_history_collection")
public class GroupChatMessageBean extends ChatMessageBean {

}
