package com.octv.im.constant;

public class ChatMessageConstant {
    /**
     * 私聊
     */
    public static final int PRIVATE_CHAT_CODE = 1;
    /**
     * 群聊消息
     */
    public static final int GROUP_CHAT_CODE = 2;
    /**
     * 系统消息
     */
    public static final int SYSTEM_MESSAGE_CODE = 3;
    /**
     * 加入或者离开聊天室
     */
    public static final int USER_GROUP_ROOM_ACTION_CODE = 4;
    /**
     * 消息状态返回
     */
    public static final int MESSAGE_STATUS_RETURN_CODE = 5;
    /**
     * 更新在线用户列表
     */
    public static final int UPDATE_USERLIST_SYSTEM_MESSGAE = 6;
    /**
     * 个人系统消息
     */
    public static final int PERSONAL_SYSTEM_MESSGAE = 7;

    /**
     * 加入当前聊天室
     */
    public static final String JOIN_CHAT_GROUP_ROOM = "1";
    /**
     * 离开当前聊天室
     */
    public static final String LEAVE_CHAT_GROUP_ROOM = "0";

    public static String PERSON_CHAT_HISTORY_COLLECTION = "person_chat_history_collection";

    public static String GROUP_CHAT_HISTORY_COLLECTION = "group_chat_history_collection";

    public static String IMAGES_TYPE_MESSAGE = "2";

    public static String TEXT_TYPE_MESSAGE = "1";
}
