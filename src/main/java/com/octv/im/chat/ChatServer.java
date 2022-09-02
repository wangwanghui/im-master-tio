package com.octv.im.chat;


import static com.octv.im.constant.SocketServerConstant.tioServerConfig;
import com.octv.im.config.ChatServerProperties;
import com.octv.im.constant.SocketServerConstant;
import com.octv.im.service.IMServerService;
import lombok.extern.slf4j.Slf4j;
import org.tio.websocket.server.WsServerStarter;
import java.io.IOException;

//聊天服务启动
@Slf4j
public class ChatServer implements Runnable {
    private int bindPort;
    private WsServerStarter wsServerStarter;

    private ChatMsgHandle chatMsgHandle;

    public ChatServer(ChatServerProperties chatServerProperties, IMServerService IMServerService) {
        bindPort = chatServerProperties.getPort();
        try {
            chatMsgHandle = new ChatMsgHandle(IMServerService);
            wsServerStarter = new WsServerStarter(bindPort, chatMsgHandle);
            tioServerConfig = wsServerStarter.getServerTioConfig();
            tioServerConfig.setName(chatServerProperties.getProtocolName());
            // server 监听器
            tioServerConfig.setServerAioListener(SocketServerConstant.chatServerListener);
            // 设置ip监控
            tioServerConfig.setIpStatListener(SocketServerConstant.chatIpListener);
            // 设置ip统计时间段
            tioServerConfig.ipStats.addDurations(SocketServerConstant.IpStatDuration.IPSTAT_DURATIONS);
            // 设置心跳超时时间
            tioServerConfig.setHeartbeatTimeout(chatServerProperties.getHeatBeatTime());
            if (chatServerProperties.getSsl().getModel() == 1) { // model 为1 SSL 认证
                // 如果你希望通过wss来访问，就加上下面的代码吧，不过首先你得有SSL证书（证书必须和域名相匹配，否则可能访问不了ssl）
                String keyStoreFile = chatServerProperties.getSsl().getKeystore();
                String trustStoreFile = chatServerProperties.getSsl().getTruststore();
                String keyStorePwd = chatServerProperties.getSsl().getPassword();
                tioServerConfig.useSsl(keyStoreFile, trustStoreFile, keyStorePwd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (wsServerStarter != null) {
            try {
                wsServerStarter.start();
                log.info("chat server start at port : {} ", bindPort);
            } catch (IOException e) {
                log.error("chat server error at port : {} ", bindPort, e);
            }
        }

    }
}
