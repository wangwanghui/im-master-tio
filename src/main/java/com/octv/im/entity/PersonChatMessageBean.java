package com.octv.im.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Document(collection = "person_chat_history_collection")
@Data
public class PersonChatMessageBean extends ChatMessageBean {

}
