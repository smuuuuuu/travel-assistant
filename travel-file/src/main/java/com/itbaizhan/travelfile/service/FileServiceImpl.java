package com.itbaizhan.travelfile.service;

import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.FileService;
import com.itbaizhan.travelfile.utils.MinioUtils;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@DubboService
@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Autowired
    private MinioUtils utils;
    @Value("${minio.bucketName}")
    private String bucketName;

    @Override
    public String upload(byte[] bytes,String name,Long userId){

        try{

            /*String name = file.getOriginalFilename();
            String filename = System.currentTimeMillis() +"."+ StringUtils.substringAfterLast(name,".");
            //文件类型
            String contentType = file.getContentType();*/
            String filename = UUID.randomUUID()+"."+ StringUtils.substringAfterLast(name,".");
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            String objectKey = "images/"+userId + "/" + filename;
            utils.uploadFile(bucketName, objectKey,bais);
            //获取外挂连接
            return utils.getPresignedObjectUrl(bucketName,filename,24 * 60 * 60); //一天
        }catch (Exception e){
            e.printStackTrace();
            throw new BusException(CodeEnum.UPLOAD_FILE_ERROR);
        }
    }

    @Override
    public String upload(byte[] bytes, String contentType, Long userId,String tripId) {
        String s = String.valueOf(System.currentTimeMillis());
        String objectKey = "trips/" + userId + "/" + tripId +"/" + s + ".md";
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(bytes)) {
            utils.putObject(bucketName,objectKey, in, bytes.length, contentType);
            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTemporaryUrl(String objectKey, Integer expireSeconds) {
        return utils.getPresignedObjectUrl(bucketName,objectKey,expireSeconds);
    }


    @Override
    public void delete(String filePath) {
        utils.removeFile(bucketName, filePath);
    }

    @Override
    public String download(String objectKey, String fileName,Integer expireSeconds) {
        return utils.getDownloadUrlByPath(bucketName, objectKey, fileName,expireSeconds);
    }
    @Override
    public void delAllObjectsByFolder(String prefix,Set<String> excludes){
        List<Item> web = utils.getAllObjectsByPrefix(bucketName, prefix, true);
        if(web != null && !web.isEmpty()){
            for (Item item : web) {
                if(excludes == null || !excludes.contains(item.objectName())){
                    delete(item.objectName());
                }
                //List<String> stringList = web.stream().map(Item::objectName).toList();
            }
        }
    }
    @Override
    public void move(String from, String to){
        if (StringUtils.isBlank(from) || StringUtils.isBlank(to) || from.equals(to)) {
            return;
        }
        utils.copyFile(bucketName, from, bucketName, to);
        utils.removeFile(bucketName, from);
    }
    /*@GetMapping("/download")
    public BaseResult downLoad(String filename, HttpServletResponse response){

        try {
            InputStream inputStream = utils.getObject(config.getBucketName(), filename);
            response.setHeader("content-Disposition","attachment;filename="+filename);
            response.setContentType("application/force-download"); //强制下载
            response.setCharacterEncoding("UTF-8");

            IOUtils.copy(inputStream,response.getOutputStream());
            return BaseResult.success();
        }catch (Exception e){
            e.printStackTrace();
            throw new BusException(CodeEnum.FILE_DOWNLOAD_ERROR);
        }
    }*/
}

