package com.atsea.zerox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.atsea.zerox.mapper")
public class ZeroXApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeroXApplication.class, args);
    }

}
