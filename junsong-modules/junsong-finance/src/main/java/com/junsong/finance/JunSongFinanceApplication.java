package com.junsong.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.junsong.common.security.annotation.EnableCustomConfig;
import com.junsong.common.security.annotation.EnableRyFeignClients;

/**
 * 财务模块
 * 
 * @author junsong
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class JunSongFinanceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunSongFinanceApplication.class, args);
        System.out.println("(◕ᴗ◕✿)  峻松财务模块启动成功  (◕ᴗ◕✿)  \n" +
                "         ██╗██╗   ██╗███╗   ██╗███████╗ ██████╗ ███╗   ██╗ ██████╗  \n"+
                "         ██║██║   ██║████╗  ██║██╔════╝██╔═══██╗████╗  ██║██╔════╝  \n"+
                "         ██║██║   ██║██╔██╗ ██║███████╗██║   ██║██╔██╗ ██║██║  ███╗ \n"+
                "    ██   ██║██║   ██║██║╚██╗██║╚════██║██║   ██║██║╚██╗██║██║   ██║ \n"+
                "    ╚█████╔╝╚██████╔╝██║ ╚████║███████║╚██████╔╝██║ ╚████║╚██████╔╝ \n"+
                "     ╚════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ \n");
    }
}
