package com.itbaizhan.traveluserapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsLoginDto {
    private String phone;
    private String code;
}
