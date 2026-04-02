package com.itbaizhan.travelmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TravelFaqMapper extends BaseMapper<TravelFaq> {

    @Update(value = "<script>" +
            "UPDATE travel_faq SET status = #{status} WHERE id IN " +
            "<foreach item='id' collection='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void status(Integer status, List<String> id);

}
