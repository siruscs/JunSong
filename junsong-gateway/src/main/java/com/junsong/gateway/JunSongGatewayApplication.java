package com.junsong.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 网关启动程序
 *
 * 网关无数据源，排除 DataSourceAutoConfiguration。
 * 幂等 Mapper 不使用 @Mapper 注解，避免被 MybatisAutoConfiguration 扫描。
 * 业务模块通过 @EnableCustomConfig -> @MapperScan("com.junsong.**.mapper") 扫描注册。
 *
 * @author ruoyi
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class JunSongGatewayApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JunSongGatewayApplication.class, args);
        System.out.println("(◕ᴗ◕✿)  网关服务启动成功  (◕ᴗ◕✿)  \n" +
                "         ██╗██╗   ██╗███╗   ██╗███████╗ ██████╗ ███╗   ██╗ ██████╗  \n"+
                "         ██║██║   ██║████╗  ██║██╔════╝██╔═══██╗████╗  ██║██╔════╝  \n"+
                "         ██║██║   ██║██╔██╗ ██║███████╗██║   ██║██╔██╗ ██║██║  ███╗ \n"+
                "    ██   ██║██║   ██║██║╚██╗██║╚════██║██║   ██║██║╚██╗██║██║   ██║ \n"+
                "    ╚█████╔╝╚██████╔╝██║ ╚████║███████║╚██████╔╝██║ ╚████║╚██████╔╝ \n"+
                "     ╚════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ \n");
    }
}
