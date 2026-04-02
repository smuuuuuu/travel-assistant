package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.pojo.TravelSensitiveWord;
import com.itbaizhan.travelcommon.service.SensitiveWordService;
import com.itbaizhan.travelmanager.mapper.SensitiveMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ManagerSensitiveWordService {

    @Autowired
    private SensitiveMapper sensitiveMapper;

    @DubboReference(cluster = "broadcast",timeout = 3000)
    private SensitiveWordService sensitiveWordService;

    public Page<TravelSensitiveWord> getSensitiveWords(Integer pageNo, Integer pageSize) {
        Page<TravelSensitiveWord> page = new Page<>(pageNo, pageSize);
        return sensitiveMapper.selectPage(page, null);
    }

    public void insert(TravelSensitiveWord sensitiveWord) {
        sensitiveMapper.insert(sensitiveWord);
        if(General.ENABLE.equals(sensitiveWord.getStatus())){
            sensitiveWordService.insert(List.of(sensitiveWord.getWord()));
        }
    }
    public void delete(List<Long> ids) {
        List<TravelSensitiveWord> travelSensitiveWords = sensitiveMapper.selectByIds(ids);
        if(travelSensitiveWords != null && !travelSensitiveWords.isEmpty()){
            sensitiveMapper.deleteByIds(ids);
            List<String> words = new ArrayList<>();
            for(TravelSensitiveWord travelSensitiveWord : travelSensitiveWords){
                if(General.ENABLE.equals(travelSensitiveWord.getStatus())){
                    words.add(travelSensitiveWord.getWord());
                }
            }
            sensitiveWordService.delete(words);
        }
    }
    public void status(List<Long> ids, Integer status) {
        List<TravelSensitiveWord> travelSensitiveWords = sensitiveMapper.selectByIds(ids);
        if (travelSensitiveWords != null && !travelSensitiveWords.isEmpty()) {
            List<String> insertWords = new ArrayList<>();
            List<String> deleteWords = new ArrayList<>();
            for (TravelSensitiveWord travelSensitiveWord : travelSensitiveWords) {
                travelSensitiveWord.setStatus(status);
                sensitiveMapper.updateById(travelSensitiveWord);
                if (General.ENABLE.equals(status)) {
                    insertWords.add(travelSensitiveWord.getWord());
                } else {
                    deleteWords.add(travelSensitiveWord.getWord());
                }
            }
            if(!insertWords.isEmpty()){
                sensitiveWordService.insert(insertWords);
            }
            if(!deleteWords.isEmpty()) {
                sensitiveWordService.delete(deleteWords);
            }
        }
    }
}
