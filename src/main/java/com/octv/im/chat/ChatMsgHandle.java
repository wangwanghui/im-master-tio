package com.octv.im.chat;

import com.octv.im.constant.SocketServerConstant;
import com.octv.im.entity.ChatMessageBean;
import com.octv.im.service.IMServerService;
import com.octv.im.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.websocket.common.WsRequest;
import org.tio.websocket.common.WsResponse;
import org.tio.websocket.common.WsSessionContext;
import org.tio.websocket.server.handler.IWsMsgHandler;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatMsgHandle implements IWsMsgHandler {
    private static List<String> userList = new ArrayList<>();
    private IMServerService IMServerService;

    public ChatMsgHandle(IMServerService IMServerService) {
        this.IMServerService = IMServerService;
    }

    /**
     * 握手时走这个方法，业务可以在这里获取cookie，request参数等
     */
    @Override
    public HttpResponse handshake(HttpRequest request, HttpResponse httpResponse, ChannelContext channelContext)
            throws Exception {
        String username = request.getParam("name");
        // 防止用户多次登入导致Tio绑定用户多次
        if (Tio.getByUserid(channelContext.tioConfig, username) == null) {
            Tio.bindUser(channelContext, username);
            Tio.bindBsId(channelContext, username);
            Tio.bindGroup(channelContext, "10");
            channelContext.setUserid(username);
            userList.add(username);
        }
        // channelContext.setToken();
        log.info("user {} connect chat server success request uri {} , channel {} ", username, request.toString(),
                channelContext);

        // 推送用户上线消息，更新客户端在线用户列表
        return httpResponse;
    }

    /**
     * @param httpRequest
     * @param httpResponse
     * @param channelContext
     * @throws Exception
     *
     */
    @Override
    public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext)
            throws Exception {
        Map<String, Object> ext = new HashMap<String, Object>();
        ext.put("userList", userList.stream().distinct().collect(Collectors.toList()));
        ChatMessageBean chatMessage = new ChatMessageBean();
        chatMessage.setExt(ext);
        chatMessage.setCode(5);
        chatMessage.setType("30");
        WsResponse wsRespo = WsResponse.fromText(JacksonUtil.writeValueAsString(chatMessage),
                SocketServerConstant.CHARSET);
        Tio.sendToGroup(channelContext.tioConfig, "10", wsRespo);

        int count = Tio.getAll(channelContext.tioConfig).getObj().size();
        // 拉取离线消息
        Set<ZSetOperations.TypedTuple<Serializable>> set = IMServerService.listOfflineMessages(channelContext);
        if (set != null && !set.isEmpty()) {
            List<ChatMessageBean> offLineMsg = new ArrayList<>();
            set.forEach(value -> {
                ChatMessageBean chatMessageBean = (ChatMessageBean) value.getValue();
                offLineMsg.add(chatMessageBean);
            });
            // 需要排序
            String jsonString = JacksonUtil.writeValueAsString(offLineMsg.stream()
                    .sorted(Comparator.comparing(ChatMessageBean::getSendTime)).collect(Collectors.toList()));
            WsResponse wsResponse = WsResponse.fromText(jsonString, WsResponse.CHARSET_NAME);
            boolean sendSuccess = Tio.send(channelContext, wsResponse);
            if (sendSuccess) {
                log.info("send to user {} {} size offline message success", channelContext.userid, offLineMsg.size());
                IMServerService.deleteOfflineMessage(channelContext, set);
            } else {
                log.error("send to user {} {} size offline message fail", channelContext.userid, offLineMsg.size());
            }
        }
    }

    /**
     * 字节消息（binaryType = arraybuffer）过来后会走这个方法
     */
    // 图片 表情
    @Override
    public Object onBytes(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
        return null;
    }

    /**
     * 当客户端发close flag时，会走这个方法
     */
    @Override
    public Object onClose(WsRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
        Tio.remove(channelContext, "receive close flag");
        Tio.unbindBsId(channelContext);
        Tio.unbindGroup(channelContext);
        userList.remove(channelContext.userid);
        log.info("user {} disconnect from sever .. ", channelContext.userid);
        return null;
    }

    /*
     * 字符消息（binaryType = blob）过来后会走这个方法
     */
    @Override
    public Object onText(WsRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
        WsSessionContext wsSessionContext = (WsSessionContext) channelContext.get();
        HttpRequest httpRequest = wsSessionContext.getHandshakeRequest();// 获取websocket握手包
        if (log.isDebugEnabled()) {
            log.debug("握手包:{}", httpRequest);
        }
        // 只要是没有带code的数据统统返回不处理
        String a = channelContext.getToken();
        if (!text.contains("code")) {
            Tio.send(channelContext, WsResponse.fromText(text, WsResponse.CHARSET_NAME));
            return null;
        }
        IMServerService.chatMessageDispatch(text, channelContext);
        return null;
    }

    public static void main(String[] args) {
        String a = "{\"token\":\"eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjEyYmFjMGQzLTcyOWEtNDZmNy1iMmRmLWU3ZTYxYjJiNDg2ZCJ9.DdspwupFg_P7tvIgwpwfrq5Rs1xFvkLOqf8qg1O8N_BHfz3EREhLBUyWua86tcU5L-VXITRHxirgUhlTaatQxQ\",\"type\":0,\"userId\":\"36b670555d2f44b89451d6a30365596d\",\"remark\":\"iOS\"}";
        System.out.println(a.length());
    }

}
