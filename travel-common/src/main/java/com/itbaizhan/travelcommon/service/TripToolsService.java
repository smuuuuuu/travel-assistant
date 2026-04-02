package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.info.DirectTrainInfo;
import com.itbaizhan.travelcommon.info.FlightDetail;
import com.itbaizhan.travelcommon.pojo.TripGaoDe;
import com.itbaizhan.travelcommon.vo.FlightVo;

import java.util.List;

public interface TripToolsService {

    List<DirectTrainInfo> getDirectTrains(TransDto trainsDto);

    List<String> getStationCode(String fromCity,String arrivalCity);

    List<FlightVo> getFlightItineraries(TransDto flightDto);

    List<TripGaoDe> getAccommodation(String city, String keywords);

    List<TripGaoDe> getScenic(String city,String keywords);

    List<TripGaoDe> getCatering(String city,String keywords);

    List<String> getIataCode(String from,String arrival);

    List<String> getAllAirport();

    List<FlightDetail> getFlight(TransDto transDto);

    List<String> getCityCode(String from,String arrival);


}