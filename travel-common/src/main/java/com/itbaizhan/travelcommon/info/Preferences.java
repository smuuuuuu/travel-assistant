package com.itbaizhan.travelcommon.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Preferences implements Serializable {
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private List<String> travelStyles;
    private List<String> foodPreferences;
    private String budgetRange;
    private List<String> activityPreferences;
    private List<String> accommodationTypes;
    private List<String> transportation;
}
