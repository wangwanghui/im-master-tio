package com.octv.im.entity;


import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
@Data
public class ChatHistoryConditionBean {
    private Integer offset;

    private Integer limit;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String from;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String to;

    //单聊发送者
    private String send;

    //单聊接受者
    private String receive;

    private String groupID;

    //查询个人消息Y N 为群聊消息
    private String personalMsg;
}
