package com.itbaizhan.travelcommon.util;

import java.util.Random;

public class RandomUtil {
    public static String buildCheckCode(int digit){
        String str = "123456789";
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for(int i = 0;i < digit;i++){
            char c = str.charAt(random.nextInt(str.length()));
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }
}
