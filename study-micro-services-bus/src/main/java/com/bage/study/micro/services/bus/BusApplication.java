package com.bage.study.micro.services.bus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@EnableAutoConfiguration
@RestController
@EnableDiscoveryClient
public class BusApplication {

  @RequestMapping("/")
  public String home() {
    return "Bus,Hello World";
  }

  public static void main(String[] args) {
    SpringApplication.run(BusApplication.class, args);
  }

}