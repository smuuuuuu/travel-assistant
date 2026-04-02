package com.itbaizhan.travel_trip_service.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travel_trip_service.config.McpMethodProperties;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.constant.TransConstant;
import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travel_trip_service.mapper.CityCodeMapper;
import com.itbaizhan.travel_trip_service.mapper.CityDirectCodeMapper;
import com.itbaizhan.travel_trip_service.mapper.FlightCityCodeMapper;
import com.itbaizhan.travel_trip_service.utils.DedupKeyUtils;
import com.itbaizhan.travel_trip_service.utils.FlightItineraryParser;
import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.info.DirectTrainInfo;
import com.itbaizhan.travelcommon.info.FlightDetail;
import com.itbaizhan.travelcommon.pojo.*;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.TripToolsService;
import com.itbaizhan.travelcommon.vo.FlightVo;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
public class TripToolsServiceImpl implements TripToolsService {
    private static final Logger log = LoggerFactory.getLogger(TripToolsServiceImpl.class);
    @Autowired
    private CityDirectCodeMapper cityDirectCodeMapper;
    @Autowired
    private List<McpAsyncClient> mcpAsyncClients;
    @Autowired
    private FlightCityCodeMapper flightCityCodeMapper;
    @Autowired
    private McpMethodProperties mcpMethodProperties;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private CityCodeMapper cityCodeMapper;

    @Override
    public List<DirectTrainInfo> getDirectTrains(TransDto trainsDto) {

        List<String> stationCode = getStationCode(trainsDto.getFromCity(), trainsDto.getToCity());

        // 如果依然没有代码，说明查询失败或城市不存在，直接返回空列表
        if (stationCode.isEmpty() || stationCode.size() < 2) {
            return new ArrayList<>();
        }

        trainsDto.setFromCity(stationCode.get(0));
        trainsDto.setToCity(stationCode.get(1));
        List<DirectTrainInfo> directTrainInfos = getTrainsFromRedis(trainsDto);
        if(!directTrainInfos.isEmpty()) {
            return directTrainInfos;
        }

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("date", trainsDto.getDate().toString());
        map.put("fromStation", stationCode.get(0));
        map.put("toStation", stationCode.get(1));
        map.put("trainFilterFlags", Objects.equals(trainsDto.getType(), TransConstant.NORMAL) ? TransConstant.NORMAL_FLAGS : TransConstant.HIGH_SPEED_FLAGS);
        map.put("format", "json");

        String jsonContent = handleMcpMethod(mcpMethodProperties.getGetTickets(), map);
        if(StringUtils.hasText(jsonContent)) {
            List<DirectTrainInfo> infos = JSON.parseArray(jsonContent, DirectTrainInfo.class);
            if (infos != null) {
                directTrainInfos.addAll(infos.stream().peek(info -> {
                    if (trainsDto.getDate().isAfter(info.getStart_date())) {
                        info.setStart_date(trainsDto.getDate());
                        info.setArrive_date(trainsDto.getDate());
                    }
                    if (info.getStart_time().isAfter(info.getArrive_time())) {
                        info.setStart_date(trainsDto.getDate());
                        info.setArrive_date(trainsDto.getDate().plusDays(1));
                    }
                    String dedupKey = DedupKeyUtils.buildTransportationDedupKey(info.getStart_train_code()
                            , LocalDateTime.of(info.getStart_date(), info.getStart_time()));
                    info.setDedupKey(dedupKey);
                }).toList());
            }
        }
        if(!directTrainInfos.isEmpty()) {
            String key;
            if(TransConstant.NORMAL.equals(trainsDto.getType())) {
                key = redisKeyProperties.buildTrainKey(trainsDto.getFromCity(), trainsDto.getToCity(), trainsDto.getDate().toString());
            }else {
                key = redisKeyProperties.buildHighKey(trainsDto.getFromCity(), trainsDto.getToCity(), trainsDto.getDate().toString());
            }
            redisTemplate.opsForValue().set(key, directTrainInfos,getRemainingMinutes(),TimeUnit.MINUTES);
            return  filterTime(trainsDto.getType(),directTrainInfos,trainsDto.getStartTime(),trainsDto.getEndTime());
        }
        return directTrainInfos;
    }
    private String handleMcpMethod(String methodName, Map<String, Object> map) {
        for (McpAsyncClient mcpAsyncClient : mcpAsyncClients) {
            try {
                McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(methodName, map);
                McpSchema.CallToolResult result = mcpAsyncClient.callTool(request).block();

                if (result != null && result.content() != null && !result.content().isEmpty()) {
                    var firstContent = result.content().get(0);
                    if (firstContent instanceof McpSchema.TextContent) {
                        String text = ((McpSchema.TextContent) firstContent).text();
                        if (!StringUtils.hasText(text)) {
                            continue;
                        }
                        String lower = text.toLowerCase(Locale.ROOT);
                        if (lower.contains("fetch failed") || lower.startsWith("error:")) {
                            log.warn("MCP tool call returned error text, methodName={}, text={}", methodName, text);
                            continue;
                        }
                        return text;
                    }
                }
            } catch (Exception e) {
                log.warn("MCP tool call failed, methodName={}", methodName, e);
            }
        }
        return "";
    }

    private String getRedisCityCode(String type, String city){
        return (String) redisTemplate.opsForHash().get(type,city);
    }
    public List<String> getAllCity(){
        return null;
    }
    private List<DirectTrainInfo> getTrainsFromRedis(TransDto transDto) {
        String key = "";

        if(TransConstant.NORMAL.equals(transDto.getType())){
            key = redisKeyProperties.buildTrainKey(transDto.getFromCity(),transDto.getToCity(),transDto.getDate().toString());
        } else if (TransConstant.HIGH_SPEED.equals(transDto.getType())) {
            key = redisKeyProperties.buildHighKey(transDto.getFromCity(),transDto.getToCity(),transDto.getDate().toString());
        } else {
            throw new BusException(CodeEnum.TRIP_TRANS_TYPE_ERROR);
        }
        List<DirectTrainInfo> directTrainInfos = (List<DirectTrainInfo>) redisTemplate.opsForValue().get(key);
        if(directTrainInfos != null){
            return filterTime(transDto.getType(),directTrainInfos,transDto.getStartTime(),transDto.getEndTime());
        }
        return new ArrayList<>();
    }
    private List<DirectTrainInfo> filterTime(Integer type,List<DirectTrainInfo> directTrainInfos,LocalTime startTime,LocalTime endTime){
        List<DirectTrainInfo> trainsList = new ArrayList<>();
        if(TransConstant.NORMAL.equals(type) || TransConstant.HIGH_SPEED.equals(type)){
            for (DirectTrainInfo directTrainInfo : directTrainInfos) {
                if(getIsBefore(startTime,directTrainInfo.getStart_time()) && getIsAfter(endTime,directTrainInfo.getArrive_time())){
                    trainsList.add(directTrainInfo);
                }
            }
        }
        return trainsList;
    }
    private boolean getIsBefore(LocalTime filterStart,LocalTime start){
        return filterStart == null || !filterStart.isAfter(start);
    }
    private boolean getIsAfter(LocalTime filterEnd,LocalTime end){
        return filterEnd == null || !filterEnd.isBefore(end);
    }
    private List<FlightDetail> getFlightFromRedis(TransDto transDto) {
        String key = redisKeyProperties.buildFlightKey(transDto.getFromCity(), transDto.getToCity(), transDto.getDate().toString());
        List<FlightDetail> flightDetails = (List<FlightDetail>) redisTemplate.opsForValue().get(key);
        if(flightDetails == null || flightDetails.isEmpty()){
            return List.of();
        }
        return flightDetails;
    }
    private List<FlightVo> getFlightVoFromRedis(TransDto transDto) {
        String key = redisKeyProperties.buildFlightItinerariesKey(transDto.getFromCity(), transDto.getToCity(), transDto.getDate().toString());
        List<FlightVo> flightVos = (List<FlightVo>) redisTemplate.opsForValue().get(key);
        if(flightVos == null || flightVos.isEmpty()){
            return List.of();
        }
        return flightVos;
    }
    @Override
    public List<String> getStationCode(String fromCity, String arrivalCity) {
        String fromCityCode = "";
        String toCityCode = "";
        fromCityCode = getRedisCityCode(redisKeyProperties.getStationCode(), fromCity);
        toCityCode = getRedisCityCode(redisKeyProperties.getStationCode(),arrivalCity);
        if (!StringUtils.hasText(fromCityCode) || !StringUtils.hasText(toCityCode)) {
            QueryWrapper<CityDirectCode> queryWrapper = new QueryWrapper<>();
            if (!StringUtils.hasText(fromCityCode)) {
                // 1. 尝试从数据库查询
                queryWrapper.eq("city",fromCity);
                CityDirectCode from = cityDirectCodeMapper.selectOne(queryWrapper);
                if (from != null) {
                    fromCityCode = from.getCode();
                }
            }
            if (!StringUtils.hasText(toCityCode)) {
                queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("city", arrivalCity);
                CityDirectCode to = cityDirectCodeMapper.selectOne(queryWrapper);
                if (to != null) {
                    toCityCode = to.getCode();
                }
            }

            // 2. 如果任一城市代码未找到，则调用 MCP 接口补充
            if (!StringUtils.hasText(fromCityCode) || !StringUtils.hasText(toCityCode)) {
                // 准备需要查询的城市名
                String queryCity1 = !StringUtils.hasText(fromCityCode) ? fromCity : null;
                String queryCity2 = !StringUtils.hasText(toCityCode) ? arrivalCity : null;

                // 调用一次 MCP 接口
                String stationCodesJson = getStationCodeFromMcp(queryCity1, queryCity2);

                // 解析 JSON: {"徐州":{"station_code":"XCH"...}, "南京":{"station_code":"NJH"...}}
                if (StringUtils.hasText(stationCodesJson)) {
                    try {
                        JSONObject jsonObject = JSON.parseObject(stationCodesJson);

                        if (!StringUtils.hasText(fromCityCode) && jsonObject.containsKey(fromCity)) {
                            fromCityCode = jsonObject.getJSONObject(fromCity).getString("station_code");
                            cityDirectCodeMapper.insert(new CityDirectCode(fromCity, fromCityCode));
                        }
                        if (!StringUtils.hasText(toCityCode) && jsonObject.containsKey(arrivalCity)) {
                            toCityCode = jsonObject.getJSONObject(arrivalCity).getString("station_code");
                            cityDirectCodeMapper.insert(new CityDirectCode(arrivalCity, toCityCode));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new BusException(CodeEnum.TRIP_FLIGHT_CODE_ERROR);
                    }
                }
            }
            redisTemplate.opsForHash().put(redisKeyProperties.getStationCode(),fromCity,fromCityCode);
            redisTemplate.opsForHash().put(redisKeyProperties.getStationCode(),arrivalCity,toCityCode);
        }
        return List.of(fromCityCode, toCityCode);
    }
    private String getStationCodeFromMcp(String cityName1, String cityName2) {

        // 构建参数
        Map<String, Object> map = new java.util.HashMap<>();

        // 拼接城市名，中间用 | 分隔
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(cityName1)) {
            sb.append(cityName1);
        }
        if (StringUtils.hasText(cityName2)) {
            if (!sb.isEmpty()) {
                sb.append("|");
            }
            sb.append(cityName2);
        }

        // 如果没有有效参数，直接返回空
        if (sb.isEmpty()) {
            return "";
        }

        map.put("stationNames", sb.toString());
        return handleMcpMethod(mcpMethodProperties.getGetStationCode(), map);
    }
    @Override
    public List<String> getIataCode(String from,String arrival){
        String fromIataCode = "";
        String toIataCode = "";
        fromIataCode = getRedisCityCode(redisKeyProperties.getIataCode(),from);
        toIataCode = getRedisCityCode(redisKeyProperties.getIataCode(),arrival);
        if(!StringUtils.hasText(fromIataCode) || !StringUtils.hasText(toIataCode)) {
            QueryWrapper<FlightCityCode> wrapper = new QueryWrapper<>();
            wrapper.eq("airport_name", from);
            FlightCityCode fromCode = flightCityCodeMapper.selectOne(wrapper);

            wrapper = new QueryWrapper<>();
            wrapper.eq("airport_name", arrival);
            FlightCityCode to = flightCityCodeMapper.selectOne(wrapper);
            if (to == null || fromCode == null) {
                return List.of();
            }
            fromIataCode = fromCode.getIataCode();
            toIataCode = to.getIataCode();
            redisTemplate.opsForHash().put(redisKeyProperties.getIataCode(),arrival,toIataCode);
            redisTemplate.opsForHash().put(redisKeyProperties.getIataCode(),from,fromIataCode);
        }
        return List.of(fromIataCode,toIataCode);
    }
    @Override
    public List<String> getCityCode(String from,String arrival){
        String fromIataCode = "";
        String toIataCode = "";
        fromIataCode = getRedisCityCode(redisKeyProperties.getCityCode(),from);
        toIataCode = getRedisCityCode(redisKeyProperties.getCityCode(),arrival);
        if(!StringUtils.hasText(fromIataCode) || !StringUtils.hasText(toIataCode)) {
            QueryWrapper<CityCode> wrapper = new QueryWrapper<>();
            wrapper.eq("city", from);
            CityCode fromCode = cityCodeMapper.selectOne(wrapper);

            wrapper = new QueryWrapper<>();
            wrapper.eq("city", arrival);
            CityCode to = cityCodeMapper.selectOne(wrapper);
            if (to == null || fromCode == null) {
                return List.of();
            }
            fromIataCode = fromCode.getCode();
            toIataCode = to.getCode();
            redisTemplate.opsForHash().put(redisKeyProperties.getCityCode(),arrival,toIataCode);
            redisTemplate.opsForHash().put(redisKeyProperties.getCityCode(),from,fromIataCode);
        }
        return List.of(fromIataCode,toIataCode);
    }
    @Override
    public List<String> getAllAirport(){
        return redisTemplate.opsForHash().keys(redisKeyProperties.getIataCode()).stream().map(String::valueOf).toList();
    }
    @Override
    public List<FlightDetail> getFlight(TransDto transDto){
        List<String> list = getIataCode(transDto.getFromCity(), transDto.getToCity());
        if(list.isEmpty()){
            throw new BusException(CodeEnum.TRIP_FLIGHT_CODE_ERROR);
        }
        transDto.setFromCity(list.get(0));
        transDto.setToCity(list.get(1));
        List<FlightDetail> flightFromRedis = getFlightFromRedis(transDto);
        if(!flightFromRedis.isEmpty()){
            return flightFromRedis;
        }
        List<FlightDetail> flightDetails = new ArrayList<>();
        Map<String,Object> map = new HashMap<>();
        map.put("dep", list.get(0));
        map.put("arr", list.get(1));
        map.put("date", transDto.getDate().toString());
        String jsonContent = handleMcpMethod(mcpMethodProperties.getSearchFlightsByDepArr(), map);
        if(StringUtils.hasText(jsonContent)) {
            try {
                JSONObject jsonObject = JSON.parseObject(jsonContent);
                // 检查 code 是否为 200
                if (jsonObject.getInteger("code") == 200 && jsonObject.containsKey("data")) {
                    List<FlightDetail> flight = JSON.parseArray(jsonObject.getString("data"), FlightDetail.class);
                    if (flight != null) {
                        flightDetails.addAll(flight.stream().peek(flightDetail -> {
                            String key = DedupKeyUtils.buildTransportationDedupKey(
                                    flightDetail.getFlightNo(), flightDetail.getFlightDeptimePlanDate());
                            flightDetail.setDedupKey(key);
                        }).toList());
                        String key = redisKeyProperties.buildFlightKey(list.get(0), list.get(1), transDto.getDate().toString());
                        redisTemplate.opsForValue().set(key, flightDetails, getRemainingMinutes(),TimeUnit.MINUTES);
                    }
                }
            } catch (Exception e) {
                log.warn("Parse flight response failed, dep={}, arr={}, date={}, raw={}",
                        list.get(0), list.get(1), transDto.getDate(), safeSnippet(jsonContent), e);
                return List.of();
            }
        }
        return flightDetails;
    }

    private String safeSnippet(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        return t.length() <= 300 ? t : t.substring(0, 300);
    }
    @Override
    public List<FlightVo> getFlightItineraries(TransDto flightDto) {
        List<String> list = getCityCode(flightDto.getFromCity(), flightDto.getToCity());
        if(list.isEmpty()){
            throw new BusException(CodeEnum.TRIP_CITY_CODE_ERROR);
        }
        flightDto.setFromCity(list.get(0));
        flightDto.setToCity(list.get(1));
        List<FlightVo> trains = getFlightVoFromRedis(flightDto);
        if(!trains.isEmpty()){
            return trains;
        }
        Map<String,Object> map = new HashMap<>();
        map.put("depCityCode", list.get(0));
        map.put("arrCityCode", list.get(1));
        map.put("depDate",flightDto.getDate().toString());

        String text = handleMcpMethod(mcpMethodProperties.getSearchFlightItineraries(), map);
        trains = FlightItineraryParser.parse(text);
        if(!trains.isEmpty()){
            trains.forEach(train -> {
                String key = DedupKeyUtils.buildTransportationDedupKey(train.getFlightNo(),
                        LocalDateTime.of(train.getDate(), train.getPlanDepTime()));
                train.setDedupkey(key);
            });
            String key = redisKeyProperties.buildFlightItinerariesKey(list.get(0), list.get(1), flightDto.getDate().toString());

            redisTemplate.opsForValue().set(key,trains,getRemainingMinutes(),TimeUnit.MINUTES); //
        }
        return trains;
    }

    private List<TripGaoDe> getGaoDeFromRedis(String key){
        Object object = redisTemplate.opsForValue().get(key);
        if(object != null){
            return (List<TripGaoDe>) object;
        }
        return List.of();
    }
    @Override
    public List<TripGaoDe> getAccommodation(String city,String keywords) {
        String key = redisKeyProperties.buildAccommodationKey(city,keywords);
        return handleGaoDe(key,city,keywords,TripConstant.ACCOMMODATION_POI_CODE);
    }

    @Override
    public List<TripGaoDe> getScenic(String city,String keywords) {
        String key = redisKeyProperties.buildScenicKey(city,keywords);
        return handleGaoDe(key,city,keywords,TripConstant.SCENIC_SPOT_POI_CODE);
    }
    @Override
    public List<TripGaoDe> getCatering(String city,String keywords){
        String key = redisKeyProperties.buildCateringKey(city,keywords);
        return handleGaoDe(key,city,keywords,TripConstant.CATERING_POI_CODE);
    }
    private List<TripGaoDe> handleGaoDe(String key, String city, String keywords, String types) {
        List<TripGaoDe> gaoDeFromRedis = getGaoDeFromRedis(key);
        if(!gaoDeFromRedis.isEmpty()){
            return gaoDeFromRedis;
        }
        Map<String,Object> map = new HashMap<>();
        map.put("keywords", keywords);
        map.put("city", city);
        map.put("types", types);

        List<TripGaoDe> gaoDeInfos = new ArrayList<>();
        String json = handleMcpMethod(mcpMethodProperties.getMapsTextSearch(), map);
        if(StringUtils.hasText(json)) {
            try {
                JSONObject jsonObject = JSON.parseObject(json);
                List<TripGaoDe> infos = JSON.parseArray(jsonObject.getString("pois"), TripGaoDe.class);
                if (infos != null) {
                    gaoDeInfos.addAll(infos.stream().peek(s -> {
                        String key1 = DedupKeyUtils.buildPoiDedupKey(s);
                        s.setDedupKey(key1);
                    }).toList());
                    redisTemplate.opsForValue().set(key,gaoDeInfos,random() ,TimeUnit.DAYS);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusException(CodeEnum.TRIP_SEARCH_ERROR);
            }
        }
        if(gaoDeInfos.isEmpty()){
            redisTemplate.opsForValue().set(key, gaoDeInfos, 60, TimeUnit.MINUTES); // 缓存空结果，防止重复请求
        }
        return gaoDeInfos;
    }

    Random random = new Random();

    public int random(){

        return 5 + random.nextInt(5);
    }
    public Long getRemainingMinutes(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().atTime(LocalTime.MIDNIGHT).plusDays(1);
        long minutes = Duration.between(now, midnight).toMinutes();
        return minutes + (long) random();
    }
}
