package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.CityCode;
import com.itbaizhan.travelcommon.pojo.CityDirectCode;
import com.itbaizhan.travelcommon.pojo.FlightCityCode;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.CodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/manager/code")
public class CodeController {
    @Autowired
    private CodeService codeService;

    @GetMapping("/cityCodePage")
    public BaseResult<Page<CityCode>> cityCodePage(@RequestParam int pageNo,@RequestParam int pageSize) {
        return BaseResult.success(codeService.cityCodePage(pageNo, pageSize));
    }
    @GetMapping("/cityCode")
    public BaseResult<CityCode> cityCode(@RequestParam String city) {
        return BaseResult.success(codeService.cityCode(city));
    }
    @PostMapping("/insertCityCode")
    public BaseResult<?> insertCityCode(@RequestBody CityCode cityCode) {
        codeService.insertCityCode(cityCode);
        return BaseResult.success();
    }
    @DeleteMapping("/deleteCityCode")
    public BaseResult<?> deleteCityCode(@RequestParam String ids, @RequestParam String cities) {

        codeService.deleteCityCode(Arrays.stream(ids.split(",")).map(Long::valueOf).toList(),
                Arrays.stream(cities.split(",")).map(String::valueOf).toList());
        return BaseResult.success();
    }

    @GetMapping("/cityDirectCodePage")
    public BaseResult<Page<CityDirectCode>> cityDirectCodePage(@RequestParam int pageNo, @RequestParam int pageSize) {
        return BaseResult.success(codeService.cityDirectCodePage(pageNo, pageSize));
    }
    @GetMapping("/cityDirectCode")
    public BaseResult<CityDirectCode> cityDirectCode(@RequestParam String city) {
        return BaseResult.success(codeService.cityDirectCode(city));
    }
    @PostMapping("/insertCityDirectCode")
    public BaseResult<?> insertCityDirectCode(@RequestBody CityDirectCode cityDirectCode) {
        codeService.insertCityDirectCode(cityDirectCode);
        return BaseResult.success();
    }
    @DeleteMapping("/deleteCityDirectCode")
    public BaseResult<?> deleteCityDirectCode(@RequestParam String ids, @RequestParam String cities) {
        codeService.deleteCityDirectCode(Arrays.stream(ids.split(",")).map(Long::valueOf).toList(),
                Arrays.stream(cities.split(",")).map(String::valueOf).toList());
        return BaseResult.success();
    }

    @GetMapping("/flightCityCodePage")
    public BaseResult<Page<FlightCityCode>> flightCityCodePage(@RequestParam int pageNo, @RequestParam int pageSize) {
        return BaseResult.success(codeService.flightCityCodePage(pageNo, pageSize));
    }
    @GetMapping("/flightCityCode")
    public BaseResult<FlightCityCode> flightCityCode(@RequestParam String city,@RequestParam String airportName) {
        return BaseResult.success(codeService.flightCityCode(city,airportName));
    }
    @PostMapping("/insertFlightCityCode")
    public BaseResult<?> insertFlightCityCode(@RequestBody FlightCityCode flightCityCode){
        codeService.insertFlightCityCode(flightCityCode);
        return BaseResult.success();
    }
    @DeleteMapping("/deleteFlightCityCode")
    public BaseResult<?> deleteFlightCityCode(@RequestParam String ids, @RequestParam String airports) {
        codeService.deleteFlightCityCode(Arrays.stream(ids.split(",")).map(Long::valueOf).toList(),
                Arrays.stream(airports.split(",")).map(String::valueOf).toList());
        return BaseResult.success();
    }
}
