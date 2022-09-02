package com.octv.im.constant;

import com.octv.im.chat.ChatIpListener;
import com.octv.im.chat.ChatServerListener;
import org.tio.server.ServerTioConfig;
import org.tio.utils.time.Time;

public class SocketServerConstant {

    /**
     * 服务的监听
     */
    public static final ChatServerListener chatServerListener = new ChatServerListener();

    /**
     * webSocket IP 监听
     */
    public static final ChatIpListener chatIpListener = new ChatIpListener();

    public static final String CHARSET = "utf-8";
    /**
     * 监听的ip
     */
    public static final String SERVER_IP = null;//null表示监听所有，并不指定ip
    /**
     * ip数据监控统计，时间段
     * @author tanyaowu
     *
     */
    public static ServerTioConfig tioServerConfig = null;

    public static interface IpStatDuration {
        public static final Long DURATION_1 = Time.MINUTE_1 * 5;
        public static final Long[] IPSTAT_DURATIONS = new Long[] { DURATION_1 };
    }


}
