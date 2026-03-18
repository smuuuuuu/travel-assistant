package com.itbaizhan.travel_trip_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itbaizhan.travelcommon.pojo.TripGaoDe;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TripGaoDeMapper extends BaseMapper<TripGaoDe> {

    List<TripGaoDe> getTripGaoDeByTripId(String tripId);

    List<String> getGaoDePoiName(Integer type);

    @Delete("""
            DELETE FROM `trip_gaoDe` g
            WHERE IFNULL(g.ref_count, 0) <= 0
              AND NOT EXISTS (
                SELECT 1 FROM trip_day_item tdi
                WHERE tdi.item_type = 'poi'
                  AND tdi.ref_id = g.`dedupKey`
              )
            LIMIT #{limit}
            """)
    int deleteOrphanByRefCount(@Param("limit") int limit);

    List<String> getNameByAiModuleId(@Param("aiModuleIds") List<Long> aiModuleIds);
}
