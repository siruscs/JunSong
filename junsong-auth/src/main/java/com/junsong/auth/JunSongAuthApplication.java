package com.junsong.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

import com.junsong.common.security.annotation.EnableCustomConfig;
import com.junsong.common.security.annotation.EnableRyFeignClients;

/**
 * 认证授权中心
 * 
 * @author junsong
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class JunSongAuthApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunSongAuthApplication.class, args);
        System.out.println("(◕ᴗ◕✿)  认证授权中心启动成功  (◕ᴗ◕✿) \n" +
                "         ██╗██╗   ██╗███╗   ██╗███████╗ ██████╗ ███╗   ██╗ ██████╗  \n"+
                "         ██║██║   ██║████╗  ██║██╔════╝██╔═══██╗████╗  ██║██╔════╝  \n"+
                "         ██║██║   ██║██╔██╗ ██║███████╗██║   ██║██╔██╗ ██║██║  ███╗ \n"+
                "    ██   ██║██║   ██║██║╚██╗██║╚════██║██║   ██║██║╚██╗██║██║   ██║ \n"+
                "    ╚█████╔╝╚██████╔╝██║ ╚████║███████║╚██████╔╝██║ ╚████║╚██████╔╝ \n"+
                "     ╚════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ \n");
    }
}
