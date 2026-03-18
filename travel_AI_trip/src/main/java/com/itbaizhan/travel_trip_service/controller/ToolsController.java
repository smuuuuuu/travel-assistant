package com.itbaizhan.travel_trip_service.controller;

import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.info.DirectTrainInfo;
import com.itbaizhan.travelcommon.info.FlightDetail;
import com.itbaizhan.travelcommon.pojo.TripGaoDe;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.service.TripToolsService;
import com.itbaizhan.travelcommon.vo.FlightVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trip/tools")
public class ToolsController {
    @Autowired
    private TripToolsService tripToolsService;

    @PostMapping("/trains")
    public BaseResult<List<DirectTrainInfo>> getDirectTrains(@RequestBody TransDto trainsDto) {
        return BaseResult.success(tripToolsService.getDirectTrains(trainsDto));
    }
    @PostMapping("/flightInfos")
    public BaseResult<List<FlightDetail>> getFlightDetails(@RequestBody TransDto flightDto) {
        return BaseResult.success(tripToolsService.getFlight(flightDto));
    }
    @PostMapping("/flights")
    public BaseResult<List<FlightVo>> getFlightItineraries(@RequestBody TransDto flightDto) {
        return BaseResult.success(tripToolsService.getFlightItineraries(flightDto));
    }

    @GetMapping("/accommodation")
    public BaseResult<List<TripGaoDe>> getAccommodation(@RequestParam String city,@RequestParam String keywords) {
        return BaseResult.success(tripToolsService.getAccommodation(city,keywords));
    }

    //@GetMapping("/ticket")
    @GetMapping("/scenic")
    public BaseResult<List<TripGaoDe>> getScenic(@RequestParam String city,@RequestParam String keywords) {
        return BaseResult.success(tripToolsService.getScenic(city,keywords));
    }
    @GetMapping("/catering")
    public BaseResult<List<TripGaoDe>> getCatering(@RequestParam String city,@RequestParam String keywords) {
        return BaseResult.success(tripToolsService.getCatering(city,keywords));
    }
    @GetMapping("/allAirport")
    public BaseResult<List<String>> getAllAirport() {
        return BaseResult.success(tripToolsService.getAllAirport());
    }

    // --- 保存接口 ---
}