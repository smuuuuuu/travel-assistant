package com.itbaizhan.travelcommon.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Activities implements Serializable {
    private String time;
    private String title;
    private String description;
    private String type; // sight, food, transport, hotel
    private String location;
    private String duration; // in minutes
    private String cost;
    private String tips;
    /*
    * {
      "time": "09:00 AM",
      "title": "Visit the Eiffel Tower",
      "description": "Explore the iconic landmark of Paris.",
      "type": "Sightseeing",
      "location": "Eiffel Tower, Paris",
      "duration": 120,
      "cost": 25.50,
      "trips": "https://www.toureiffel.paris/en"
    }
    * */
}
