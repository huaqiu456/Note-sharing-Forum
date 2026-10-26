package com.huaqiu.xiaohashu.note;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.huaqiu.xiaohashu.note.mapper")
@EnableFeignClients(basePackages = "com.huaqiu")
public class XiaohashuNoteBizApplication {

    public static void main(String [] args){
        SpringApplication.run(XiaohashuNoteBizApplication.class,args);
    }
}
