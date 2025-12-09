package com.nofee.api.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.nofee.api.test.trustscore.mapper", "com.nofee.api.test.carrierintegration.mapper"})
public class CarrierTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarrierTestApplication.class, args);
    }
}
