package com.itbaizhan.travelcommon.util;

import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import lombok.SneakyThrows;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JWTUtil {
    public final static String PRIVATE_JSON = "{\"kty\":\"RSA\",\"n\":\"10LqS5Rlb4R8awVp-CjG9wog3MScQmv6iHELKk5BxkG7oFpvVjUZxURVpS9CVslgUW3Ahxw4nqAqgkfAB7xqQKvQXCxYEKVnCqNMvMwQ8CcSjwwevZRSmxXC0iqYb7-IAzMWRQt1L8MCsCcVL4NadeXAG7Mcj9MhUCBRdDdtJE0CTyQChlYCqhBpwIYU1ZAMNtm180qcBZUbLnivW6c2NPDfUPeQeLukuhVNGe_pGVjpkmRJ1cftI13IAHwAcKQlVd-eigjoOTFHbd_0dJPNUL--HMoiZkRii8a6aDC7R04-ulwV0GpjYQ28nTDbujBlTq7cg2vCxMtqYYB_LCweKw\",\"e\":\"AQAB\",\"d\":\"UsSg7-SkSAfKCyBuyYNsVWqojhpu9r8r-rB0mZSzh7aYnSSIsL4EJayLvdL5EAOoLEcmAw1f2-Y70hIijRbuR-WIXRiT1_qxLw7xH5rbXGu5oBUJ8N0KEiWA3myQZk9fzdMj2fJ44Z969h5g7_7ylmh7wGrTLZ4BrhxvFV-GcLES68FXehrZs26Wd3lqrQ4EcGzUytO29M4ug2OzAM2gXBOD-WIJ6ZwzJffsHzcm-VuGiRiE3-zGjK0gGNEv4atWVS8r5HH6Njgt-zeABkiR1VRhbBa3ImMSWiSkR6A_820DAlynfnmd6MN4lx-WyFnqj3gzQBg5mGlJeDww9usGOQ\",\"p\":\"4cR92G5XvlTf1yrzBLyCQFiyZIp9UhAo3awJ9rhnMG1szwxVM80mNDG-U7Zw_fVK36zPAXNwtcoX7qCHt4Jezzg6AwcdnmKR7m4uz2EQ0G0QlMZlD64R9dPUkbQshfh3KK9ExD_DWj9csIZ7bBm3lDDvA6WTwqW0fLiDrSmq47c\",\"q\":\"9BZDbOp8tZ_jDJ16vYo60Q3649DtiUPeyADPWdZ0tMiaqseGPQ7_-IAuZc429Z6w2Zqosnixv-SeazMZSZpLEO1DlMo6NUOdpidEXlhvc13BExWOzfi8YNHsQyzYApjMjowTKjzb2_hr3CRHjkmnsSbS7cf_LV-5IVrJOFd0oS0\",\"dp\":\"Sw3CdGACZXDb1TEfBxUQDAowjAIKNWzP2RmnWlDlZRELmi1UadsqdzGOP1AochTIpmFRk_7nmler6xWM6LG7iRH9HyKPcyb7spMIlsKDD3ciiwMFd9f7eojIYwPNxV_bZTgXGVLFQ8xKqRicEUzfXbGC0-iIna3uuYOxqrBucjM\",\"dq\":\"oRagYR1QOL3Qpt63xqeXsai8T-XvHoxAV1bRgcTbOQS7rsIHgfyISoSuGlpCQ0_7_2DmvQKSrMZBFxaeNckyiXVNCMF-MPDy9lkr8egDhev2JJYxrRXfnTZt6teJqA0X02v2qgWIRGBrLtpyryFFHtE0m-FGM7fdAnpcGmb9ffE\",\"qi\":\"mKfFzwD0DPvx6vkGYmDgHUOWkqw7UjoG_Jk7gUJ4SGmRRpMIAtkcr_j3C015gJOyxzxyzwvpKrex4dq6Ii-WZd0DkxxP3F_zeGoIyCsMxolC_MRa7pKcn9wOWOs_vK-3ExvcMuLZ8Z7bQIJoR2PXg6n6oKC54i2AM0M2A-SIrl0\"}";
    public final static String PUBLIC_JSON="{\"kty\":\"RSA\",\"n\":\"10LqS5Rlb4R8awVp-CjG9wog3MScQmv6iHELKk5BxkG7oFpvVjUZxURVpS9CVslgUW3Ahxw4nqAqgkfAB7xqQKvQXCxYEKVnCqNMvMwQ8CcSjwwevZRSmxXC0iqYb7-IAzMWRQt1L8MCsCcVL4NadeXAG7Mcj9MhUCBRdDdtJE0CTyQChlYCqhBpwIYU1ZAMNtm180qcBZUbLnivW6c2NPDfUPeQeLukuhVNGe_pGVjpkmRJ1cftI13IAHwAcKQlVd-eigjoOTFHbd_0dJPNUL--HMoiZkRii8a6aDC7R04-ulwV0GpjYQ28nTDbujBlTq7cg2vCxMtqYYB_LCweKw\",\"e\":\"AQAB\"}";

    /**
     * 生成token
     *
     * @param userId  用户id
     * @param username 用户名字
     * @return
     */
    @SneakyThrows
    public static String sign(Long userId, String username) {
        // 1、 创建jwtclaims jwt内容载荷部分
        JwtClaims claims = new JwtClaims();
        // 是谁创建了令牌并且签署了它
        claims.setIssuer("itbaizhan");
        // 令牌将被发送给谁
        claims.setAudience("audience");
        // 失效时间长 （分钟）
        claims.setExpirationTimeMinutesInTheFuture(60 * 24);
        // 令牌唯一标识符
        claims.setGeneratedJwtId();
        // 当令牌被发布或者创建现在
        claims.setIssuedAtToNow();
        // 再次之前令牌无效
        claims.setNotBeforeMinutesInThePast(2);
        // 主题
        claims.setSubject("shopping");
        // 可以添加关于这个主题得声明属性
        claims.setClaim("userId", userId);
        claims.setClaim("username", username);
        // 2、签名
        JsonWebSignature jws = new JsonWebSignature();
        //赋值载荷
        jws.setPayload(claims.toJson());
        // 3、jwt使用私钥签署
        PrivateKey privateKey = new RsaJsonWebKey(JsonUtil.parseJson(PRIVATE_JSON)).getPrivateKey();
        jws.setKey(privateKey);
        // 4、设置关键 kid
        jws.setKeyIdHeaderValue("keyId");
        // 5、设置签名算法
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        // 6、生成jwt
        String jwt = jws.getCompactSerialization();
        return jwt;
    }
    /**
     * 解密token，获取token中的信息
     *
     * @param token
     */
    @SneakyThrows
    public static Map<String, Object> verify(String token){
        // 1、引入公钥
        PublicKey publicKey = new RsaJsonWebKey(JsonUtil.parseJson(PUBLIC_JSON)).getPublicKey();
        // 2、使用jwtcoonsumer 验证和处理jwt
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                //过期时间
                .setAllowedClockSkewInSeconds(30) //允许在验证 得时候留有一些余地 计算时钟偏差 秒
                .setRequireSubject() // 主题 生命
                .setExpectedIssuer("itbaizhan") // jwt需要知道 谁发布得 用来验证发布人
                .setExpectedAudience("audience") //jwt目的是谁 用来验证观众
                .setVerificationKey(publicKey) // 用公钥验证签名 验证密钥
                .setJwsAlgorithmConstraints(
                        new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_USING_SHA256))
                .build();
        // 3、验证jwt 并将其处理为 claims
        try {
            JwtClaims jwtClaims = jwtConsumer.processToClaims(token);
            return jwtClaims.getClaimsMap();
        }catch (Exception e){
            throw new BusException(CodeEnum.USER_ERROR);
        }
    }
    public static void main(String[] args){
        // 生成
        String baizhan = sign(1001L, "baizhan");
        System.out.println(baizhan);
        Map<String, Object> stringObjectMap = verify(baizhan);
        System.out.println(stringObjectMap.get("userId"));
        System.out.println(stringObjectMap.get("username"));
    }
}
