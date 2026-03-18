package com.itbaizhan.travel_trip_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "method")
public class McpMethodProperties {
    private String getStationCode;
    private String getTickets;
    private String searchFlightItineraries;
    private String searchFlightsByDepArr;
    private String mapsTextSearch;
    private String mapsAroundSearch;
    private String mapsWeather;

    public List<String> getPoiTools(){
        String prePoi = "spring_ai_mcp_client_amap_maps_";
        return List.of(prePoi + mapsTextSearch, prePoi + mapsAroundSearch, prePoi + mapsWeather);
    }

}