package com.itbaizhan.travel_ai_service.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.itbaizhan.travelcommon.info.DailyForecast;
import com.itbaizhan.travelcommon.info.WeatherForecast;
import com.itbaizhan.travelcommon.info.WeatherInfo;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class WeatherService{
    //public static final String BASE_URL = "https://restapi.amap.com/v3/weather/weatherInfo";
    private RestTemplate restTemplate;
    public WeatherService() {
        this.restTemplate = new RestTemplate();
    }
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${redis.current}")
    private String current;
    @Value("${redis.further}")
    private String further;

    @Value("${amap.web.key}")
    private String amapKey;

    @Tool(description = "获取指定经纬度的实时天气和未来3天天气预报")
    public WeatherInfo apply(double longitude, double latitude) {
        WeatherInfo currentWeather = getCurrentWeather(longitude, latitude);
        WeatherForecast weatherForecast = getWeatherForecast(longitude, latitude);

        currentWeather.setWeatherForecast(weatherForecast);
        return currentWeather;
    }
    /**
     * 获取天气预报（未来3天）
     */
    @Tool(description = "获取指定经纬度的未来3天天气预报")
    public WeatherForecast getWeatherForecast(double longitude, double latitude) {

        String cityCode = getWeatherByLocation(longitude, latitude);
        String furtherKey = further + cityCode;
        String value = redisTemplate.opsForValue().get(furtherKey);
        if (value != null) {
            return JSON.parseObject(value, WeatherForecast.class);
        }

        String url = String.format(
                "https://restapi.amap.com/v3/weather/weatherInfo?key=%s&city=%s&extensions=all",
                amapKey, cityCode
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                WeatherForecast weatherForecast = parseWeatherForecast(json);
                redisTemplate.opsForValue().set(furtherKey, JSON.toJSONString(weatherForecast),3, TimeUnit.HOURS);
                return weatherForecast;
            } else {
                //throw new RuntimeException("天气预报查询失败: " + json.getString("info"));
                //throw new BusException(200,"返回结果为空,请检查传入参数");
                return null;
            }
        } catch (Exception e) {
            //throw new RuntimeException("获取天气预报失败", e);
            //throw new BusException(500,"Error getWeatherForecast： " + e.getMessage());
            return null;
        }
    }
    /**
     * 获取实时天气
     */
    @Tool(description = "获取指定经纬度的实时天气")
    public WeatherInfo getCurrentWeather(double longitude, double latitude) {
        String cityCode = getWeatherByLocation(longitude, latitude);

        String currentKey = current + cityCode;
        String value = redisTemplate.opsForValue().get(currentKey);
        if (value != null) {
            return JSON.parseObject(value, WeatherInfo.class);
        }
        String url = String.format(
                "https://restapi.amap.com/v3/weather/weatherInfo?key=%s&city=%s&extensions=base",
                amapKey, cityCode
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONObject lives = json.getJSONArray("lives").getJSONObject(0);
                WeatherInfo weatherInfo = parseWeatherInfo(lives);
                redisTemplate.opsForValue().set(currentKey, JSON.toJSONString(weatherInfo),30, TimeUnit.MINUTES);
                return weatherInfo;
            } else {
                //throw new BusException(200,"返回结果为空,请检查传入参数");
                return null;
            }
        } catch (Exception e) {
            //throw new BusException(500,"Error getWeatherForecast： " + e.getMessage());
            return null;
        }
    }
    /**
     * 根据经纬度获取城市编码
     */
    public String getWeatherByLocation(double longitude, double latitude) {
        // 1. 逆地理编码获取城市编码
        String url = String.format(
                "https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%f,%f",
                amapKey, longitude, latitude
        );
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = JSON.parseObject(response);

        if ("1".equals(json.getString("status"))) {
            JSONObject addressComponent = json.getJSONObject("regeocode")
                    .getJSONObject("addressComponent");
            String cityCode = addressComponent.getString("adcode");

            // 2. 获取城市编码
            return cityCode;
        } else {
           // throw new BusException(CodeEnum.AI_WEATHER_LOCATION_ERROR);
            return null;
        }
    }

    /**
     * 解析实时天气数据
     */
    private WeatherInfo parseWeatherInfo(JSONObject lives) {
        WeatherInfo weather = new WeatherInfo();
        weather.setProvince(lives.getString("province"));
        weather.setCity(lives.getString("city"));
        weather.setAdcode(lives.getString("adcode"));
        weather.setWeather(lives.getString("weather"));
        weather.setTemperature(lives.getString("temperature"));
        weather.setWindDirection(lives.getString("winddirection"));
        weather.setWindPower(lives.getString("windpower"));
        weather.setHumidity(lives.getString("humidity"));
        weather.setReportTime(lives.getString("reporttime"));

        // 添加天气图标
        weather.setWeatherIcon(getWeatherIcon(weather.getWeather()));

        return weather;
    }
    /**
     * 解析天气预报数据
     */
    private WeatherForecast parseWeatherForecast(JSONObject json) {
        WeatherForecast forecast = new WeatherForecast();
        JSONObject forecasts = json.getJSONArray("forecasts").getJSONObject(0);

        forecast.setCity(forecasts.getString("city"));
        forecast.setAdcode(forecasts.getString("adcode"));
        forecast.setProvince(forecasts.getString("province"));
        forecast.setReportTime(forecasts.getString("reporttime"));

        // 解析每天预报
        List<DailyForecast> dailyList = new ArrayList<>();
        JSONArray casts = forecasts.getJSONArray("casts");

        for (int i = 0; i < casts.size(); i++) {
            JSONObject cast = casts.getJSONObject(i);
            DailyForecast daily = new DailyForecast();

            daily.setDate(cast.getString("date"));
            daily.setWeek(cast.getString("week"));
            daily.setDayWeather(cast.getString("dayweather"));
            daily.setNightWeather(cast.getString("nightweather"));
            daily.setDayTemp(cast.getString("daytemp"));
            daily.setNightTemp(cast.getString("nighttemp"));
            daily.setDayWind(cast.getString("daywind"));
            daily.setNightWind(cast.getString("nightwind"));
            daily.setDayPower(cast.getString("daypower"));
            daily.setNightPower(cast.getString("nightpower"));

            // 添加天气图标
            daily.setDayIcon(getWeatherIcon(daily.getDayWeather()));
            daily.setNightIcon(getWeatherIcon(daily.getNightWeather()));

            dailyList.add(daily);
        }

        forecast.setForecasts(dailyList);
        return forecast;
    }
    /**
     * 根据天气描述获取图标
     */
    private String getWeatherIcon(String weather) {
        if (weather == null) return "❓";

        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("晴", "☀️");
        iconMap.put("多云", "⛅");
        iconMap.put("阴", "☁️");
        iconMap.put("小雨", "🌦️");
        iconMap.put("中雨", "🌧️");
        iconMap.put("大雨", "⛈️");
        iconMap.put("暴雨", "🌧️");
        iconMap.put("雷阵雨", "⛈️");
        iconMap.put("雪", "❄️");
        iconMap.put("雾", "🌫️");
        iconMap.put("霾", "😷");
        iconMap.put("风", "💨");

        return iconMap.getOrDefault(weather, "🌤️");
    }

    /**
     * 获取穿衣建议
     */
    public String getDressingAdvice(String temperature) {
        if (temperature == null) return "请关注实时天气";

        try {
            int temp = Integer.parseInt(temperature);

            if (temp >= 30) {
                return "天气炎热，建议穿短袖、短裤、裙子等夏季服装，注意防晒";
            } else if (temp >= 25) {
                return "天气较热，建议穿短袖、薄外套等夏季服装";
            } else if (temp >= 20) {
                return "天气舒适，建议穿长袖、薄外套等春秋装";
            } else if (temp >= 15) {
                return "天气凉爽，建议穿长袖、外套、长裤等";
            } else if (temp >= 10) {
                return "天气较冷，建议穿毛衣、外套、厚裤等";
            } else if (temp >= 0) {
                return "天气寒冷，建议穿棉衣、羽绒服、厚裤等冬季服装";
            } else {
                return "天气严寒，建议穿厚羽绒服、保暖内衣、帽子手套等";
            }
        } catch (NumberFormatException e) {
            return "请关注实时天气变化";
        }
    }
}
