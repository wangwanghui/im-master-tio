package com.octv.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "octv.im-chat-server")
@Data
public class ChatServerProperties {

    private Ssl ssl;

    private Integer port;

    private Long offLineMessageLimit;

    private Long heatBeatTime;

    private String protocolName;

    private String[] uploadImagesType;

    private String uploadImagesPath;

    private Boolean openCluster;

    @Data
    public static class Ssl {
        private Integer model;
        private String keystore;
        private String truststore;
        private String password;
    }
}
