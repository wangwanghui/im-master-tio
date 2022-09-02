package com.octv.im.chat;

import com.octv.im.config.ChatServerProperties;
import com.octv.im.service.IMServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
@Slf4j
public class ChatService implements InitializingBean {
    @Autowired
    private ChatServerProperties chatServerProperties;

    @Autowired
    private Executor executor;

    @Autowired
    private IMServerService IMServerService;

    @Override
    public void afterPropertiesSet() throws Exception {
        ChatServer chatServer = new ChatServer(chatServerProperties, IMServerService);
        executor.execute(chatServer);
    }
}
