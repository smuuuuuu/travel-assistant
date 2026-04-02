package com.itbaizhan.travel_trip_service.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.itbaizhan.travel_trip_service.mapper.SensitiveMapper;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.pojo.TravelSensitiveWord;
import com.itbaizhan.travelcommon.util.SensitiveTextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SensitiveConfig {
    @Autowired
    private SensitiveMapper sensitiveMapper;

    @Bean
    public SensitiveTextUtil replace(SensitiveWordBs sensitiveWordBs){
        return new SensitiveTextUtil(sensitiveWordBs);
    }

    //在系统启动时加载敏感词到内存中
    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance()
                .wordDeny(WordDenys.chains(WordDenys.defaults() ,    //默认敏感词（黑名单）
                        new IWordDeny() {                           //自定义敏感词（黑名单）
                            @Override
                            public List<String> deny() {
                                QueryWrapper<TravelSensitiveWord> queryWrapper = new QueryWrapper<>();
                                queryWrapper.eq("status", General.ENABLE);
                                return sensitiveMapper.selectList(queryWrapper)
                                        .stream()
                                        .map(TravelSensitiveWord::getWord)
                                        .toList();
                            }
                        }
                ))
                .ignoreCase(true) //忽略大小写
                .ignoreWidth(true) //忽略半角全角
                .enableEmailCheck(true)
                .enableUrlCheck(true)         //启动url和邮箱检查。
                .enableNumCheck(true) //数字检查
                .init();
    }
}
