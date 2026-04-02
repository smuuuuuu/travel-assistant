package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "travel_sensitive_word")
public class TravelSensitiveWord {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    @TableField(value = "word")
    private String word;

    @TableField(value = "status")
    private Integer status;
}
