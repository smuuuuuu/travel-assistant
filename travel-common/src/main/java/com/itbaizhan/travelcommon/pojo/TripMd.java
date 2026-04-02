package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value ="trip_md")
public class TripMd  implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(value = "trip_id")
    private String tripId;
    @TableField(value = "user_id")
    private Long userId;
    @TableField(value = "object_key")
    private String objectKey;
    @TableField(value = "create_at")
    private LocalDateTime createAt;
    @TableField(value = "update_at")
    private LocalDateTime updateAt;
}
