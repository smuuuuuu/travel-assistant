package com.itbaizhan.travelcommon.AiSessionDto;

import lombok.Data;

@Data
public class ChatDto {
    private String question;
    private String current;
    private String tripId;
    private Integer isBackup;
}
