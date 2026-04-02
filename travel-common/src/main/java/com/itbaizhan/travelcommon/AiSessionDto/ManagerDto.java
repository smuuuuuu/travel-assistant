package com.itbaizhan.travelcommon.AiSessionDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ManagerDto {
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String role;
}
