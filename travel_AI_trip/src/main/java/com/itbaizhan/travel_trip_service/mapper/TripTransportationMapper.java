package com.itbaizhan.travel_trip_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itbaizhan.travelcommon.pojo.TripTransportation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface TripTransportationMapper extends BaseMapper<TripTransportation> {
    @Delete("""
            DELETE FROM trip_transportation tt
            WHERE IFNULL(tt.ref_count, 0) <= 0
              AND NOT EXISTS (
                SELECT 1 FROM trip_day_item tdi
                WHERE tdi.item_type = 'transport'
                  AND tdi.ref_id = tt.`dedupKey`
              )
            LIMIT #{limit}
            """)
    int deleteOrphanByRefCount(@Param("limit") int limit);
}
