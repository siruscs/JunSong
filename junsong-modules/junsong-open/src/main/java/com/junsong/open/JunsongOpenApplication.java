package com.junsong.open;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.junsong.common.security.annotation.EnableCustomConfig;
import com.junsong.common.security.annotation.EnableRyFeignClients;

/**
 * 开放平台模块
 *
 * @author junsong
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class JunsongOpenApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunsongOpenApplication.class, args);
        System.out.println("(◕ᴗ◕✿) 开放平台模块启动成功 (◕ᴗ◕✿)\n" +
                "         ██╗██╗   ██╗███╗   ██╗███████╗ ██████╗ ███╗   ██╗ ██████╗  \n" +
                "         ██║██║   ██║████╗  ██║██╔════╝██╔═══██╗████╗  ██║██╔════╝  \n" +
                "         ██║██║   ██║██╔██╗ ██║███████╗██║   ██║██╔██╗ ██║██║  ███╗ \n" +
                "    ██   ██║██║   ██║██║╚██╗██║╚════██║██║   ██║██║╚██╗██║██║   ██║ \n" +
                "    ╚█████╔╝╚██████╔╝██║ ╚████║███████║╚██████╔╝██║ ╚████║╚██████╔╝ \n" +
                "     ╚════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝  \n");
    }
}
