package com.itbaizhan.travelmanager.dto;

import lombok.Data;

@Data
public class PromptUpsertRequest {
    private String content;
    private String description;
}
