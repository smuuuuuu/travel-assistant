package com.itbaizhan.travelcommon.managerDto;

import lombok.Data;

@Data
public class GaoDeAiModuleDto {
    private Long id;
    private String moduleKey;
    private String promptId;
    private String description;
    private Integer isEnabled;
    private Integer type;
    private String name;
}
