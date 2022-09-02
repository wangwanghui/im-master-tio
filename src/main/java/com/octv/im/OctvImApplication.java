package com.octv.im;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tio.utils.jfinal.P;

@SpringBootApplication
@EnableRabbit
public class OctvImApplication {

    public static void main(String[] args) {
        // 加载属性文件
        P.use("application.yml");
        SpringApplication.run(OctvImApplication.class, args);
    }

}
