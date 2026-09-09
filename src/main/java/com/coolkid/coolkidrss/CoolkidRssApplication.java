package com.coolkid.coolkidrss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

@SpringBootApplication(scanBasePackages = {"com.coolkid"})
@EnableScheduling
@EnableCaching
public class CoolkidRssApplication implements Serializable {

	public static void main(String[] args) {
		SpringApplication.run(CoolkidRssApplication.class, args);
	}
}
