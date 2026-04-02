package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.CityCode;
import com.itbaizhan.travelcommon.pojo.CityDirectCode;
import com.itbaizhan.travelcommon.pojo.FlightCityCode;
import com.itbaizhan.travelmanager.config.RedisKeyProperties;
import com.itbaizhan.travelmanager.mapper.CityCodeMapper;
import com.itbaizhan.travelmanager.mapper.CityDirectCodeMapper;
import com.itbaizhan.travelmanager.mapper.FlightCityCodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Transactional
@Service
public class CodeService {
    @Autowired
    private CityCodeMapper cityCodeMapper;
    @Autowired
    private CityDirectCodeMapper cityDirectCodeMapper;
    @Autowired
    private FlightCityCodeMapper flightCityCodeMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;

    public Page<CityCode> cityCodePage(int pageNo,int pageSize) {
        Page<CityCode> page = new Page<>(pageNo, pageSize);
        return cityCodeMapper.selectPage(page, null);
    }
    public CityCode cityCode(String city) {
        QueryWrapper<CityCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city", city);
        return cityCodeMapper.selectOne(queryWrapper);
    }
    public void insertCityCode(CityCode cityCode) {
        cityCodeMapper.insert(cityCode);
        //同步到redis
        redisTemplate.opsForHash().put(redisKeyProperties.getCityCode(),cityCode.getCity(),cityCode.getCode());
    }
    public void deleteCityCode(List<Long> ids, List<String> cities) {
        cityCodeMapper.deleteByIds(ids);
        redisTemplate.opsForHash().delete(redisKeyProperties.getCityCode(),cities);
    }

    public Page<CityDirectCode> cityDirectCodePage(int pageNo, int pageSize) {
        Page<CityDirectCode> page = new Page<>(pageNo, pageSize);
        return cityDirectCodeMapper.selectPage(page, null);
    }
    public CityDirectCode cityDirectCode(String city) {
        QueryWrapper<CityDirectCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("city", city);
        return cityDirectCodeMapper.selectOne(queryWrapper);
    }
    public void insertCityDirectCode(CityDirectCode cityDirectCode) {
        cityDirectCodeMapper.insert(cityDirectCode);
        //同步到redis
        redisTemplate.opsForHash().put(redisKeyProperties.getStationCode(),cityDirectCode.getCity(),cityDirectCode.getCode());
    }
    public void deleteCityDirectCode(List<Long> ids, List<String> cities) {
        cityDirectCodeMapper.deleteByIds(ids);
        redisTemplate.opsForHash().delete(redisKeyProperties.getStationCode(),cities);
    }

    public Page<FlightCityCode> flightCityCodePage(int pageNo, int pageSize) {
        Page<FlightCityCode> page = new Page<>(pageNo, pageSize);
        return flightCityCodeMapper.selectPage(page, null);
    }
    public FlightCityCode flightCityCode(String cityName,String airportName) {
        QueryWrapper<FlightCityCode> queryWrapper = new QueryWrapper<>();
        if(StringUtils.hasText(airportName)) {
            queryWrapper.eq("airport_name", airportName);
        }
        if(StringUtils.hasText(cityName)) {
            queryWrapper.eq("city_name", cityName);
        }
        return flightCityCodeMapper.selectOne(queryWrapper);
    }
    public void insertFlightCityCode(FlightCityCode flightCityCode) {
        flightCityCodeMapper.insert(flightCityCode);
        redisTemplate.opsForHash().put(redisKeyProperties.getIataCode(), flightCityCode.getAirportName(), flightCityCode.getIataCode());
    }
    public void deleteFlightCityCode(List<Long> ids, List<String> airports) {
        flightCityCodeMapper.deleteByIds(ids);
        redisTemplate.opsForHash().delete(redisKeyProperties.getIataCode(), airports);
    }
}
