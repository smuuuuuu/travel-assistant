package com.itbaizhan.travelfile;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelfile.config.MinioConfig;
import com.itbaizhan.travelfile.service.FileServiceImpl;
import com.itbaizhan.travelfile.utils.MinioUtils;
import io.minio.messages.Item;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@SpringBootTest
class TravelFileApplicationTests {


    @Autowired
    private MinioUtils minioUtils;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Autowired
    private FileServiceImpl fileServiceImpl;
    @Test
    void contextLoads() {
        /*List<Item> web = minioUtils.getAllObjectsByPrefix(bucketName, "web", true);
        for (Item item : web) {
            System.out.println(item);
            System.out.println(item.objectName());
        }
        fileServiceImpl.delete("web/JavaScript.pdf");*/
        /*String s = "web/CSS3.pdf";
        fileServiceImpl.move(s,"websaa/CSS3.pdf");*/
        String from = "5/9ce674a9-a416-4a74-9589-c88c33dbe8d0/1773491160434.md";
        String to   = "5/c4d8254c-c0cc-4ecb-8bdb-712b8abe73a6/1773491160434.md";
        fileServiceImpl.move(from, to);
    }
}
