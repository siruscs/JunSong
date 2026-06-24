package com.junsong.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.junsong.common.security.annotation.EnableCustomConfig;
import com.junsong.common.security.annotation.EnableRyFeignClients;

/**
 * 工作流引擎模块（Flowable 8.0.0 · Spring Boot 4 微服务）
 *
 * 与主体共用同一套 Spring Cloud 框架（Nacos 注册 / 配置 / Sentinel / 网关），
 * 通过 @EnableRyFeignClients 直接消费 junsong-api-system 等远程服务。
 *
 * @author junsong
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class JunSongWorkflowApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunSongWorkflowApplication.class, args);
        System.out.println("(◕ᴗ◕✿)  工作流模块启动成功  (◕ᴗ◕✿)\n" +
                "  ____                       _\n" +
                " / ___| ___ _ __   ___  ___ (_)___\n" +
                "| |  _ / _ \\ '_ \\ / _ \\/ __|| / __|\n" +
                "| |_| |  __/ | | |  __/\\__ \\| \\__ \\\n" +
                " \\____|\\___|_| |_|\\___||___/|_|___/   workflow engine on Flowable 8.x\n");
    }
}
