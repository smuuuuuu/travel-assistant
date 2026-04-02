package com.itbaizhan.travelmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ManagerLoginResponse {
    private String token;
    private long expiresInSeconds;
}
