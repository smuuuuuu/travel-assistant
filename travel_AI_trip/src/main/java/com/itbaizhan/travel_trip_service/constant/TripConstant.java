package com.itbaizhan.travel_trip_service.constant;


public class TripConstant {
    public static final Integer BACKUP = 1;
    public static final Integer NO_BACKUP = 0;

    public static final Integer SAVE = 1;
    public static final Integer NO_SAVE = 0;

    public static final Integer ACCOMMODATION_TYPE = 1; //住宿
    public static final Integer SCENIC_SPOT_TYPE = 0; //景点
    public static final Integer CATERING_TYPE = 2;//餐饮

    public static final String ACCOMMODATION_POI_CODE = "100000";
    public static final String SCENIC_SPOT_POI_CODE = "110000";
    public static final String CATERING_POI_CODE = "050000";

    public static final String ACCOMMODATION = "accommodation";
    public static final String SCENIC = "scenic";
    public static final String CATERING = "catering";


    public static final String TRANS = "transport";
    public static final String POI = "poi";

    public static final Integer DRAFT_TRIP_ID = 1;
    public static final Integer GENERATE_TRIP_ID = 0;
    public static final Integer MODIFY_TRIP_ID = 2;
    public static final Integer PROCESSING_MD_ID = 3;
    public static final Integer Complete_trip_ID = 4;
    /*public final static String MONGO_MODIFY_PROMPT = "prompt_modify_plan";

    public final static String MONGO_GENERATE_PROMPT = "prompt_generate";

    public final static String MONGO_TOOL_RESTRICTION_PROMPT = "prompt_tool_restriction";

    public final static String MONGO_ALL_MODIFY_PROMPT = "prompt_all_modify";*/
}
