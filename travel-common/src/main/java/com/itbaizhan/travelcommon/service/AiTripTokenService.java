package com.itbaizhan.travelcommon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiTripToken;

/**
* @author smuuuu
* @description 针对表【ai_trip_token】的数据库操作Service
* @createDate 2026-03-23 12:05:04
*/
public interface AiTripTokenService {

    /**
     * 管理端分页查询（条件均可为 null 表示忽略）
     */
    IPage<AiTripToken> pageForAdmin(Page<AiTripToken> page, Long userId, String tripId, Integer type);

    /**
     * 管理端汇总 use_token（条件均可为 null 表示忽略）
     */
    long sumUseTokenForAdmin(Long userId, String tripId, Integer type);
}
