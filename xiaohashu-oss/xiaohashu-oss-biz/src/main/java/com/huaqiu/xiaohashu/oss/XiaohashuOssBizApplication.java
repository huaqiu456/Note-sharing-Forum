package com.huaqiu.xiaohashu.oss;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.huaqiu.xiaohashu.oss.mapper")
public class XiaohashuOssBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohashuOssBizApplication.class, args);
    }

}

