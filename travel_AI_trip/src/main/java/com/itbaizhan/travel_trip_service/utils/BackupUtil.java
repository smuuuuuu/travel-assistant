package com.itbaizhan.travel_trip_service.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BackupUtil {

    /**
     * 将 JSON 文本按 UTF-8 编码后进行 GZIP 压缩，返回压缩后的字节数组。
     */
    public static byte[] gzipJson(String json) throws IOException {
        if (json == null) {
            return null;
        }
        byte[] input = json.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(input);
        }
        return bos.toByteArray();
    }

    /**
     * 将 GZIP 压缩后的 UTF-8 JSON 字节数组解压并还原成字符串。
     */
    public static String ungzipToJson(byte[] gzipped) throws IOException {
        if (gzipped == null) {
            return null;
        }
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gzipped));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = gis.read(buffer)) >= 0) {
                bos.write(buffer, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    public static String getTravelPlanResponseJson(byte[] bytes,Integer format) throws IOException {
        String json;
        if (format == 1) {
            json = new String(bytes, StandardCharsets.UTF_8);
        } else {
            json = BackupUtil.ungzipToJson(bytes);
        }
        return json;
    }
}