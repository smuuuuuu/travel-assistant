package com.itbaizhan.travel_trip_service.utils;

import com.itbaizhan.travelcommon.pojo.TripGaoDe;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DedupKeyUtils {
    private DedupKeyUtils() {}

    public static DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 构造交通条目的去重键。
     * 规则：trainNumber + ":" + departureTime(yyyyMMddHHmmss)
     */
    public static String buildTransportationDedupKey(String trainNumber, LocalDateTime departureTime) {

        if (!StringUtils.hasText(trainNumber) || departureTime == null) {
            return null;
        }
        return trainNumber + ":" + departureTime.format(formatter2);
    }

    /**
     * 构造 POI 条目的去重键。
     * 规则：优先 gd:{poiId}；否则 p3:{location(6位)}:{tel11/typecode/_}
     */
    public static String buildPoiDedupKey(TripGaoDe tripGaoDe) {
        if (tripGaoDe == null || !StringUtils.hasText(tripGaoDe.getLocation())) {
            return null;
        }
        String poiIdNorm = normalizeKeyPart(tripGaoDe.getPoiId(), 80);
        if (StringUtils.hasText(poiIdNorm)) {
            return "gd:" + poiIdNorm;
        }

        String locationKey = normalizePoiLocation(tripGaoDe.getLocation());
        String tel11 = normalizePhone11(tripGaoDe.getTel());
        String typecode = normalizeKeyPart(tripGaoDe.getTypecode(), 20);

        String suffix = StringUtils.hasText(tel11) ? tel11 : typecode;
        if (!StringUtils.hasText(suffix)) {
            suffix = "_";
        }
        return "p3:" + locationKey + ":" + suffix;
    }

    private static String normalizePoiLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        if (!StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        String[] parts = trimmed.split(",");
        if (parts.length != 2) {
            return trimmed.replace(" ", "");
        }
        BigDecimal lng = parseDecimal(parts[0]);
        BigDecimal lat = parseDecimal(parts[1]);
        if (lng == null || lat == null) {
            return trimmed.replace(" ", "");
        }
        String lngStr = lng.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        String latStr = lat.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return lngStr + "," + latStr;
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizePhone11(String tel) {
        if (!StringUtils.hasText(tel)) {
            return null;
        }
        String digits = tel.replaceAll("\\D+", "");
        if (digits.length() >= 11) {
            return digits.substring(0, 11);
        }
        return digits.isEmpty() ? null : digits;
    }

    private static String normalizeKeyPart(String s, int maxLen) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        String cleaned = s.trim().replaceAll("\\s+", "");
        cleaned = cleaned.replace(":", "").replace("|", "");
        if (cleaned.length() > maxLen) {
            return cleaned.substring(0, maxLen);
        }
        return cleaned;
    }
}
