package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.itbaizhan.travelcommon.util.TagDeserializer;
import lombok.Data;

/**
 *
 * @TableName trip_gaoDe
 */
@TableName(value ="trip_gaoDe", autoResultMap = true)
@Data
public class TripGaoDe implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "poi_id")
    private String poiId;
    /**
     *
     */
    @TableField(value = "type")
    private Integer type;

    @TableField(exist = false)
    private String typeName;

    /**
     *
     */
    @TableField(value = "name")
    private String name;

    /**
     *
     */
    @TableField(value = "p_name")
    private String pName;

    /**
     *
     */
    @TableField(value = "city_name")
    private String cityName;

    /**
     *
     */
    @TableField(value = "address")
    private String address;

    /**
     *
     */
    @TableField(value = "typecode")
    private String typecode;

    /**
     *
     */
    @TableField(value = "rating")
    private String rating;

    /**
     *
     */
    @TableField(value = "cost")
    @JsonDeserialize(using = TagDeserializer.class)
    private String cost;

    /**
     *
     */
    @TableField(value = "tag")
    @JsonDeserialize(using = TagDeserializer.class)
    private String tag;

    /**
     *
     */
    @TableField(value = "location")
    private String location;

    /**
     *
     */
    @TableField(value = "entr_location")
    private String entrLocation;
    private String tel;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Photo photo;

    @TableField(value = "create_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createAt;

    @TableField(value = "dedupKey")
    private String dedupKey;

    @TableField(value = "ref_count")
    private Integer refCount;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @JsonDeserialize(using = TagDeserializer.class)
    public void setCost(String cost) {
        this.cost = cost;
    }

    @JsonDeserialize(using = TagDeserializer.class)
    public void setTag(String tag) {
        this.tag = tag;
    }

     @Data
     public static class Photo implements Serializable {
        private String title;
        private String url;

         public Photo() {
         }

         public Photo(String title, String url) {
             this.title = title;
             this.url = url;
         }
     }
}
