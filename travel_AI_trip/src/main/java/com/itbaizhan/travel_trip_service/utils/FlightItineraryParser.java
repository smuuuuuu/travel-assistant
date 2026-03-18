package com.itbaizhan.travel_trip_service.utils;

import com.itbaizhan.travelcommon.vo.FlightVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightItineraryParser {

    /**
     * 解析 searchFlightItineraries 返回的文本数据，提取航班信息
     * @param text MCP 工具返回的自然语言文本
     * @return 提取出的航班列表
     */
    public static List<FlightVo> parse(String text) {
        List<FlightVo> flightInfos = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return flightInfos;
        }

        // 正则表达式匹配模式
        // 匹配格式如：航班号：MU2881，起飞时间：2026-01-17 07:25:00，到达时间：2026-01-17 08:25:00，耗时：1h，无需中转，经济舱价格：1056元
        String regex = "航班号：([A-Z0-9]+)，起飞时间：(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})，到达时间：(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})，耗时：([^，]+)，无需中转，(.*?)价格：(\\d+(?:\\.\\d+)?)元";
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                FlightVo info = new FlightVo();
                info.setFlightNo(matcher.group(1));
                info.setPlanDepTime(getLocalTime(matcher.group(2)));
                info.setDate(getLocaldate(matcher.group(2)));
                info.setPlanArrTime(getLocalTime(matcher.group(3)));
                info.setDuration(matcher.group(4));
                info.setSeatClass(matcher.group(5));
                String priceStr = matcher.group(6);
                if (priceStr != null && !priceStr.isEmpty()) {
                    info.setPrice(Double.parseDouble(priceStr));
                }

                // 设置一些默认状态，因为这个接口不返回详细状态
                info.setStatus("计划");

                flightInfos.add(info);
            } catch (Exception e) {
                // 忽略解析错误的单条记录
                System.err.println("解析航班信息出错: " + e.getMessage());
            }
        }

        return flightInfos;
    }

    public static LocalTime getLocalTime(String dateTimeStr) {
        // 推荐做法
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dt = LocalDateTime.parse(dateTimeStr, formatter);
        return dt.toLocalTime();
    }
    public static LocalDate getLocaldate(String dateStr) {
        // 推荐做法
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dt = LocalDateTime.parse(dateStr, formatter);
        return dt.toLocalDate();
    }
}