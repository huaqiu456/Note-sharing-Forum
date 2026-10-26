package com.huaqiu.xiaohashu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.huaqiu.xiaohashu.user.biz.mapper")
@EnableFeignClients(basePackages = "com.huaqiu.xiaohashu")
public class XiaohashuUserBizApplication {

    public static void main(String[] args){
        SpringApplication.run(XiaohashuUserBizApplication.class,args);
    }
}
