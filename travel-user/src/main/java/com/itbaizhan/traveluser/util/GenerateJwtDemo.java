package com.itbaizhan.traveluser.util;

import com.alibaba.fastjson2.JSON;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

import java.util.HashMap;
import java.util.Map;

public class GenerateJwtDemo {
    public static void main(String[] args) throws JoseException {
        /*RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        final String publicKeyString = rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        final String privateKeyString = rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
        System.out.println(publicKeyString);
        System.out.println(privateKeyString);*/

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("userId", 1001L);
        claims.put("username", "baizhan");
        Map<String,String> user = new HashMap<>();
        user.put("userId", "1001");
        user.put("username", "baizhan");
        claims.put("user", user);
        String jsonString = JSON.toJSONString(claims);
        System.out.println(jsonString);
    }
}
