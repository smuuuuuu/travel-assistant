package com.itbaizhan.travelmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "manager.jwt")
public class ManagerJwtProperties {
    /**
     * HS256 密钥，生产环境务必通过环境变量覆盖。
     */
    private String secret = "change-me-manager-jwt-secret-min-32-chars!!";
    private long expirationHours = 12;
}
