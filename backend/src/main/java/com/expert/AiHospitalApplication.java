package com.expert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI智慧医院智能导诊系统 启动类
 */
@SpringBootApplication
@MapperScan("com.expert.mapper")
public class AiHospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHospitalApplication.class, args);
    }
}
