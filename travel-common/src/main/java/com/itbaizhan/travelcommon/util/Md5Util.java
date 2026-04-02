package com.itbaizhan.travelcommon.util;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5Util {
    public final static String md5Key = "BAIZHAN"; //密钥

    public static String encode(String text){
        return DigestUtils.md5Hex(text + md5Key);
    }
    public static boolean verify(String text,String cipher){
        String encode = encode(text);
        if(encode.equalsIgnoreCase(cipher)){
            return true;
        }
        return false;
    }
}
