package com.junsong.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.junsong.common.security.annotation.EnableCustomConfig;
import com.junsong.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 * 
 * @author junsong
 */
@EnableCustomConfig
@EnableRyFeignClients
@EnableScheduling
@SpringBootApplication
public class JunSongSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunSongSystemApplication.class, args);
        System.out.println("(◕ᴗ◕✿) 系统服务模块启动成功  (◕ᴗ◕✿) \n" +
                "         ██╗██╗   ██╗███╗   ██╗███████╗ ██████╗ ███╗   ██╗ ██████╗  \n"+
                "         ██║██║   ██║████╗  ██║██╔════╝██╔═══██╗████╗  ██║██╔════╝  \n"+
                "         ██║██║   ██║██╔██╗ ██║███████╗██║   ██║██╔██╗ ██║██║  ███╗ \n"+
                "    ██   ██║██║   ██║██║╚██╗██║╚════██║██║   ██║██║╚██╗██║██║   ██║ \n"+
                "    ╚█████╔╝╚██████╔╝██║ ╚████║███████║╚██████╔╝██║ ╚████║╚██████╔╝ \n"+
                "     ╚════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ \n");

    }
}
