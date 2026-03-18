package com.itbaizhan.travel_trip_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.Trips;
import com.itbaizhan.travelcommon.vo.TripVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;


/**
* @author smuuuu
* @description 针对表【trips(行程计划表)】的数据库操作Mapper
* @createDate 2025-10-19 22:34:48
* @Entity generator.domain.Trips
*/
public interface TripsMapper extends BaseMapper<Trips> {

    Page<TripVo> getTripVoByUserId(Page<TripVo> page, @Param("userId") Long userId);

    Integer isHaveDirect(String trainNumber,String startTime);

    @Delete("""
            DELETE FROM trip_backup WHERE expire_at < now() LIMIT 2000;
            """)
    void deleteExpireBackup();

    Integer getCompleteStatusInteger(String tripId);

    Integer getIsPresentTrip(String tripId,Long userId);
}




