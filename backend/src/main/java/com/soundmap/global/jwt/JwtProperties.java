package com.soundmap.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 soundmap.jwt.* 와 매핑된다. */
@ConfigurationProperties(prefix = "soundmap.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
