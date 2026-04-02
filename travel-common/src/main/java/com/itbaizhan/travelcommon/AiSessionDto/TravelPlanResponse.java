package com.itbaizhan.travelcommon.AiSessionDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.itbaizhan.travelcommon.info.Activities;
import com.itbaizhan.travelcommon.pojo.*;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class TravelPlanResponse implements Serializable {
    private String tripId;
    private String title;
    private String description;
    private String departure;
    private String destination;
    private BigDecimal budget;
    private BigDecimal totalPrice;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonDeserialize(using = MultiFormatLocalDateTimeDeserializer.class)
    private LocalDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonDeserialize(using = MultiFormatLocalDateTimeDeserializer.class)
    private LocalDateTime endDate;
    private Integer totalDays;
    private Integer travelerCount;
    private Integer completeStatus;
    private String notes;
    private List<String> travelStyle;
    private List<DayPlan> days;
    private List<TripGaoDe> gaoDes;
    private List<TripTransportation> transportations;

    private Integer isSave;

    @Data
    public static class DayPlan {
        private Long id;
        private String date;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateTime;
        private String title;
        private String description;
        private BigDecimal budgetPlanned;
        private TripSchedules.WeatherInfo weather;
        private List<TripDayItem> items;
    }

    public static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter SPACE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private static final DateTimeFormatter SPACE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter T_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        private static final DateTimeFormatter T_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                String raw = p.getValueAsString();
                if (raw == null) return null;
                String text = raw.trim();
                if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;

                if (text.endsWith("Z") || text.contains("+") || (text.length() >= 22 && text.lastIndexOf('-') > 10)) {
                    return OffsetDateTime.parse(text).toLocalDateTime();
                }

                try {
                    return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ignored) {
                }
                try {
                    return LocalDateTime.parse(text, SPACE_SECOND);
                } catch (Exception ignored) {
                }
                try {
                    return LocalDateTime.parse(text, SPACE_MINUTE);
                } catch (Exception ignored) {
                }
                try {
                    return LocalDateTime.parse(text, T_SECOND);
                } catch (Exception ignored) {
                }
                try {
                    return LocalDateTime.parse(text, T_MINUTE);
                } catch (Exception ignored) {
                }
                try {
                    return LocalDate.parse(text).atStartOfDay();
                } catch (Exception ignored) {
                }

                return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text, "Unsupported datetime format");
            } catch (Exception e) {
                return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, p.getValueAsString(), e.getMessage());
            }
        }
    }

}
