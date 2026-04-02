package com.itbaizhan.travelcommon.service;

import java.util.Set;

public interface FileService {

    String upload(byte[] bytes, String name,Long userId);

    String upload(byte[] bytes,String contentType,Long userId,String tripId);

    String getTemporaryUrl(String objectKey, Integer expireSeconds);

    void delete(String filePath);

    String download(String objectKey,String fileName,Integer expireSeconds);

    void delAllObjectsByFolder(String prefix, Set<String> excludes);

    void move(String from, String to);
}
