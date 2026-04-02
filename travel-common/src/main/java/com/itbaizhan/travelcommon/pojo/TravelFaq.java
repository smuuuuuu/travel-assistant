package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("travel_faq")
public class TravelFaq implements Serializable {

    @TableId(value = "id",type = IdType.INPUT)
    private String id;
    private Integer type;
    private String question;
    private String answer;
    private Integer status; //1启用 0禁用
    private String city;
    @TableField(value = "use_count")
    private Integer useCount;
}
