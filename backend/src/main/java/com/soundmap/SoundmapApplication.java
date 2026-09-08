package com.soundmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// JWT 로 인증하므로 Boot 가 만드는 인메모리 계정(기동 로그의 generated security password)은 쓰지 않는다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SoundmapApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundmapApplication.class, args);
	}
}
