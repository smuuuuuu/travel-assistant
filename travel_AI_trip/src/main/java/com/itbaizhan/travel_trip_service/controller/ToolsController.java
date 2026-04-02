package com.itbaizhan.travel_trip_service.controller;

import com.itbaizhan.travel_trip_service.utils.VerifyUtil;
import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.info.DirectTrainInfo;
import com.itbaizhan.travelcommon.info.FlightDetail;
import com.itbaizhan.travelcommon.pojo.TripGaoDe;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.TripToolsService;
import com.itbaizhan.travelcommon.util.SensitiveTextUtil;
import com.itbaizhan.travelcommon.vo.FlightVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trip/tools")
public class ToolsController {
    @Autowired
    private TripToolsService tripToolsService;
    @Autowired
    private VerifyUtil verifyUtil;
    @Autowired
    private SensitiveTextUtil sensitiveTextUtil;

    @PostMapping("/trains")
    public BaseResult<List<DirectTrainInfo>> getDirectTrains(@RequestBody TransDto trainsDto) {
        sensitiveTextUtil.checkTransDto(trainsDto);
        if (verifyUtil.transDto(trainsDto)) {
            return BaseResult.success(tripToolsService.getDirectTrains(trainsDto));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }
    @PostMapping("/flightInfos")
    public BaseResult<List<FlightDetail>> getFlightDetails(@RequestBody TransDto flightDto) {
        sensitiveTextUtil.checkTransDto(flightDto);
        if (verifyUtil.flightDto(flightDto)) {
            return BaseResult.success(tripToolsService.getFlight(flightDto));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }
    @PostMapping("/flights")
    public BaseResult<List<FlightVo>> getFlightItineraries(@RequestBody TransDto flightDto) {
        sensitiveTextUtil.checkTransDto(flightDto);
        if (verifyUtil.flightDto(flightDto)) {
            return BaseResult.success(tripToolsService.getFlightItineraries(flightDto));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }

    @GetMapping("/accommodation")
    public BaseResult<List<TripGaoDe>> getAccommodation(@RequestParam String city,@RequestParam String keywords) {
        sensitiveTextUtil.checkCityAndKeywords(city, keywords);
        if(verifyUtil.poiVerify(city, keywords)) {
            return BaseResult.success(tripToolsService.getAccommodation(city,keywords));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }

    //@GetMapping("/ticket")
    @GetMapping("/scenic")
    public BaseResult<List<TripGaoDe>> getScenic(@RequestParam String city,@RequestParam String keywords) {
        sensitiveTextUtil.checkCityAndKeywords(city, keywords);
        if(verifyUtil.poiVerify(city, keywords)) {
            return BaseResult.success(tripToolsService.getScenic(city,keywords));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }
    @GetMapping("/catering")
    public BaseResult<List<TripGaoDe>> getCatering(@RequestParam String city,@RequestParam String keywords) {
        sensitiveTextUtil.checkCityAndKeywords(city, keywords);
        if(verifyUtil.poiVerify(city, keywords)) {
            return BaseResult.success(tripToolsService.getCatering(city,keywords));
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR);
    }
    @GetMapping("/allAirport")
    public BaseResult<List<String>> getAllAirport() {
        return BaseResult.success(tripToolsService.getAllAirport());
    }

    // --- 保存接口 ---
}