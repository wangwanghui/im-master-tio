package com.octv.im.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class ChatMessageBean implements Serializable {
    private static final long serialVersionUID = 9638471483021559L;

    @Id
    private String msgId;

    @Transient
    private Integer code;

    @Indexed
    @Field("send_user_id")
    private String sendUserId;

    @Field("receive_user_id")
    @Indexed
    private String receiverUserId;

    @Field("send_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    // 图片或者文字 value = I/images or T/text
    @Transient
    private String type;

    private String msg;

    private Map<String, Object> ext;

    @Field("user_name")
    private String username;

    private String groupAction;
    @Indexed
    @Field("group_id")
    private String groupId;

    @Field("group_name")
    private String groupName;

    @Field("read_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;

    @Field("error_msg")
    private String errorMsg;

    private Integer status;

    private String filePath;

    @Transient
    private MultipartFile file;
}
