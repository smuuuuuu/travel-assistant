package com.itbaizhan.travel_trip_service.utils;

import com.itbaizhan.travelcommon.service.FileService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MdUtils {
    @DubboReference
    private FileService fileService;

    public String uploadMd(byte[] bytes,String contentType,Long userId,String tripId) {
        return fileService.upload(bytes, contentType, userId,tripId);
    }
    public String getTemporaryUrlMd(String objectKey) {
        return fileService.getTemporaryUrl(objectKey, 60 * 60 * 24); // 1天有效期
    }
    public void deleteMd(String objectKey) {
        fileService.delete(objectKey);
    }
    public String getDownloadUrlByPath(String objectKey,String fileName){
        return fileService.download(objectKey,fileName,60*60);
    }
    public void delAllObjectsByFolder(String folder, Set<String> excludes){
        fileService.delAllObjectsByFolder(folder,excludes);
    }
    public void move(String from, String to){
        fileService.move(from,to);
    }
}
