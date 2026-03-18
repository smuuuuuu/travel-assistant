package com.itbaizhan.travel_trip_service.tool;

import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.info.DirectTrainInfo;
import com.itbaizhan.travelcommon.info.FlightDetail;
import com.itbaizhan.travelcommon.service.TripToolsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.util.List;

@Component
public class CodeTools {

    @Autowired
    private TripToolsService tripToolsService;


    /**
     * 查询火车/高铁可购买车次列表（直达），支持按时间段过滤。
     * @param fromCity 出发城市/车站名（中文）
     * @param toCity 到达城市/车站名（中文）
     * @param date 出发日期（yyyy-MM-dd）
     * @param type 交通类型：火车=0，高铁=1
     * @param startTime 最早出发时间（HH:mm:ss，可为 null）
     * @param endTime 最晚到达时间（HH:mm:ss，可为 null）
     * @return 直达车次列表
     */
    @Tool(description = "查询火车/高铁可购买车次（直达）。返回字段常用：start_train_code(车次), from_station, to_station, start_date, start_time, arrive_date, arrive_time, lishi(历时), prices[].seat_name/num/price, dedupKey。映射建议：transportations.trainNumber=start_train_code；departureLocation=from_station；arrivalLocation=to_station；departureTime=start_date+start_time；arrivalTime=arrive_date+arrive_time；seatClass 从 prices[] 选可售座席。")
    public List<DirectTrainInfo> getDirectTrains(@ToolParam(description = "出发城市/车站名中文") String fromCity,
                                                 @ToolParam(description = "到达城市/车站名中文") String toCity,
                                                 @ToolParam(description = "出发日期(yyyy-MM-dd)") String date,
                                                 @ToolParam(description = "0火车/1高铁") Integer type,
                                                 @ToolParam(description = "最早出发时间") String startTime,
                                                 @ToolParam(description = "最晚到达时间") String endTime){
        TransDto dto = new TransDto();
        dto.setFromCity(fromCity);
        dto.setToCity(toCity);
        dto.setDate(parseLocalDate(date));
        dto.setType(type);
        dto.setStartTime(parseLocalTimeNullable(startTime));
        dto.setEndTime(parseLocalTimeNullable(endTime));
        return tripToolsService.getDirectTrains(dto);
    }

    /**
     * 使用出发城市和到达城市、出发日期搜索可购买的航班选项和最低价格（城市三字码逻辑由服务内部处理）。
     * @param fromCity 出发城市名（中文）
     * @param toCity 到达城市名（中文）
     * @param date 出发日期（yyyy-MM-dd）
     * @return 航班选项列表
     */
    /*@Tool(description = "使用出发城市和到达城市、出发日期搜索可购买的航班选项和最低价格。参数：fromCity(出发城市中文), toCity(到达城市中文), date(yyyy-MM-dd)。注意：不需要传 startTime/endTime/type。映射建议：从返回里挑选 1 条可落地方案写入 transportations/trainNumber 等字段，并在某天 items 里引用。")
    public List<FlightVo> getFlights(String fromCity, String toCity, String date){
        TransDto dto = new TransDto();
        dto.setFromCity(fromCity);
        dto.setToCity(toCity);
        dto.setDate(parseLocalDate(date));
        try {
            return tripToolsService.getFlightItineraries(dto);
        } catch (Exception e) {
            return List.of();
        }
    }*/

    /**
     * 使用出发机场和到达机场、出发日期查询航班（机场三字码逻辑由服务内部处理）。
     * @param fromAirport 出发机场名（中文）
     * @param toAirport 到达机场名（中文）
     * @param date 出发日期（yyyy-MM-dd）
     * @return 航班详情列表
     */
    @Tool(description = "使用出发机场和到达机场、出发日期查询航班。注意：不需要传 startTime/endTime/type。映射建议：transportations.trainNumber=FlightNo；departureLocation=FlightDepAirport；arrivalLocation=FlightArrAirport；departureTime=FlightDeptimePlanDate；arrivalTime=FlightArrtimePlanDate；description 包含 FlightCompany/FlightDuration。")
    public List<FlightDetail> getFlightDetails(@ToolParam(description = "出发机场中文") String fromAirport,
                                               @ToolParam(description = "到达机场中文") String toAirport,
                                               @ToolParam(description = "出发日期(yyyy-MM-dd)") String date){
        TransDto dto = new TransDto();
        dto.setFromCity(fromAirport);
        dto.setToCity(toAirport);
        dto.setDate(parseLocalDate(date));
        try {
            return tripToolsService.getFlight(dto);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析 yyyy-MM-dd 日期字符串为 LocalDate。
     * @param date 日期字符串（yyyy-MM-dd）
     * @return LocalDate；若参数为空则返回 null
     */
    private LocalDate parseLocalDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 解析 HH:mm:ss 时间字符串为 LocalTime。
     * @param time 时间字符串（HH:mm:ss，可为 null）
     * @return LocalTime；若参数为空则返回 null
     */
    private LocalTime parseLocalTimeNullable(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String v = time.trim();
        if (v.length() == 5) {
            v = v + ":00";
        }
        return LocalTime.parse(v, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * 中文城市名查询代表该城市的 station_code
     * @param fromCity 出发城市
     * @param arrivalCity 到达城市
     * @return 用 | 分隔的两个station_code字符串（如 XCH|NJH）
     */
    /*@Tool(description = "中文城市名/车站名查询 station_code。需要同时提供出发与到达城市/车站名。返回格式固定为：出发station_code|到达station_code，例如 XCH|NJH。")
    public String getStationCode(String fromCity, String arrivalCity) {
        List<String> list = tripToolsService.getStationCode(fromCity, arrivalCity);
        StringBuilder sb = new StringBuilder();
        sb.append(list.get(0));
        sb.append("|");
        sb.append(list.get(1));
        return sb.toString();
    }*/

    /**
     * 根据机场名获取机场三字码（IATA 3-letter codes）
     * @param from 出发机场名
     * @param arrival 到达机场名
     * @return 用 | 分隔的两个机场三字码字符串（如 PEK|SHA）
     *//*
    @Tool(description = "根据机场名获取机场三字码（IATA 3-letter codes）。需要同时提供出发机场和到达机场名称。返回格式固定为：出发三字码|到达三字码，例如 PEK|SHA。")
    public String getIataCode(String from, String arrival) {
        List<String> list = tripToolsService.getIataCode(from, arrival);
        StringBuilder sb = new StringBuilder();
        sb.append(list.get(0));
        sb.append("|");
        sb.append(list.get(1));
        return sb.toString();
    }

    *//**
     * 根据城市名获取城市三字码（IATA 3-letter codes）
     * @param from 出发城市名
     * @param arrival 到达城市名
     * @return 用 | 分隔的两个城市三字码字符串（如 BJS|SHA）
     *//*
    @Tool(description = "根据城市名获取城市三字码（IATA 3-letter codes）。需要同时提供出发城市和到达城市名称。返回格式固定为：出发三字码|到达三字码，例如 BJS|SHA。")
    public String getCityCode(String from, String arrival) {
        List<String> list = tripToolsService.getCityCode(from, arrival);
        StringBuilder sb = new StringBuilder();
        sb.append(list.get(0));
        sb.append("|");
        sb.append(list.get(1));
        return sb.toString();
    }*/

    /**
     * 解析 MCP searchFlightItineraries 返回的原始内容（可能是完整 JSON，也可能是 data 文本），输出结构化航班列表 JSON 数组
     * @param rawContent MCP 返回的原始字符串（完整 JSON 或 data 文本）
     * @return 航班列表 JSON 数组字符串，元素字段包含 flightNo/departureTime/arrivalTime/duration/seatClass/price/status
     */
    /*@Tool(description = "解析 MCP searchFlightItineraries 的返回内容（完整 JSON 或其中 data 文本），输出结构化航班列表 JSON 数组。每条包含：flightNo, departureTime(yyyy-MM-dd HH:mm:ss), arrivalTime(yyyy-MM-dd HH:mm:ss), duration, seatClass, price, status。")
    public String parseFlightItineraries(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "[]";
        }

        String text = rawContent;
        try {
            JSONObject jsonObject = JSON.parseObject(rawContent);
            if (jsonObject != null && jsonObject.containsKey("data")) {
                String dataText = jsonObject.getString("data");
                if (dataText != null && !dataText.isBlank()) {
                    text = dataText;
                }
            }
        } catch (Exception ignored) {
        }

        List<FlightVo> flightVos = FlightItineraryParser.parse(text);
        if (flightVos == null || flightVos.isEmpty()) {
            return "[]";
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> result = new ArrayList<>();
        for (FlightVo flightVo : flightVos) {
            if (flightVo == null) {
                continue;
            }

            LocalDate date = flightVo.getDate();
            LocalDateTime departure = (date == null || flightVo.getPlanDepTime() == null)
                    ? null
                    : LocalDateTime.of(date, flightVo.getPlanDepTime());

            LocalDateTime arrival = null;
            if (date != null && flightVo.getPlanArrTime() != null) {
                LocalDate arrivalDate = date;
                if (flightVo.getPlanDepTime() != null && flightVo.getPlanArrTime().isBefore(flightVo.getPlanDepTime())) {
                    arrivalDate = date.plusDays(1);
                }
                arrival = LocalDateTime.of(arrivalDate, flightVo.getPlanArrTime());
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("flightNo", flightVo.getFlightNo());
            item.put("departureTime", departure == null ? null : departure.format(dateTimeFormatter));
            item.put("arrivalTime", arrival == null ? null : arrival.format(dateTimeFormatter));
            item.put("duration", flightVo.getDuration());
            item.put("seatClass", flightVo.getSeatClass());
            item.put("price", flightVo.getPrice());
            item.put("status", flightVo.getStatus());
            result.add(item);
        }

        return JSON.toJSONString(result);
    }*/
}
