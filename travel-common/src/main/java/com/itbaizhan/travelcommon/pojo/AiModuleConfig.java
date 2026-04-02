package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_module_config")
public class AiModuleConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(value = "module_key")
    private String moduleKey;
    @TableField(value = "prompt_id")
    private String promptId;
    @TableField(value = "description")
    private String description;
    @TableField(value = "is_enabled")
    private Integer isEnabled;
    @TableField(value = "is_tool")
    private Integer isTool;
}
