package com.itbaizhan.travel_trip_service.config;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuLongFilterConfig {

    /**
     * Hutool {@link BitMapBloomFilter#BitMapBloomFilter(int)} 的 m 不是「位数」。
     * 内部每个子过滤器位图大小为 {@code (m/5) × 1024 × 1024 × 8} bit，且必须 ≤ {@link Integer#MAX_VALUE}，
     * 否则会触发 {@code Assert.checkBetween}（"between 1 and 2147483647"）。
     * 因此 m=2000 时 (2000/5)×8Mi 位超过 int 上限，启动失败。
     * m 至少为 5 才能使 (m/5)≥1；更大 m 可提高容量、降低误判率（更占内存）。
     */
    @Bean
    public BitMapBloomFilter cityDirectCodeBloomFilter() {
        return new BitMapBloomFilter(5);
    }

    @Bean
    public BitMapBloomFilter flightCityCodeMapperBloomFilter() {
        return new BitMapBloomFilter(25);
    }

    @Bean
    public BitMapBloomFilter gaodePoiBloomFilter() {
        return new BitMapBloomFilter(50);
    }
}
