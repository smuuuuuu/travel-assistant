package com.itbaizhan.travelcommon.util;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.itbaizhan.travelcommon.AiSessionDto.ChatDto;
import com.itbaizhan.travelcommon.AiSessionDto.TransDto;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanRequest;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;

import java.util.List;

/**
 * 基于 houbb sensitive-word 的文本违规检测，命中敏感词时抛出 {@link BusException}。
 */
public class SensitiveTextUtil {

    private SensitiveWordBs BS;

    private SensitiveTextUtil() {
    }

    public SensitiveTextUtil(SensitiveWordBs BS) {
        this.BS = BS;
    }

    public boolean containsSensitive(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return BS.contains(text);
    }

    public void add(List<String> words){
        this.BS.addWord(words);
    }

    public void delete(List<String> words){
        this.BS.removeWord(words);
    }

    /**
     * 将若干段用户输入拼接后检测；任一段命中敏感词即拒绝。
     */
    public void checkUserInput(String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts != null) {
            for (String p : parts) {
                if (p != null && !p.isBlank()) {
                    sb.append(p);
                }
            }
        }
        if (sb.isEmpty()) {
            return;
        }
        if (BS.contains(sb.toString())) {
            throw new BusException(CodeEnum.CONTENT_VIOLATION);
        }
    }

    public void checkTravelPlanRequest(TravelPlanRequest r) {
        if (r == null) {
            return;
        }
        String styles = "";
        List<String> travelStyle = r.getTravelStyle();
        if (travelStyle != null && !travelStyle.isEmpty()) {
            styles = String.join(",", travelStyle);
        }
        checkUserInput(
                r.getDestination(),
                r.getDeparture(),
                r.getBudget(),
                r.getRawRequirement(),
                r.getTransportation(),
                r.getFlightDepAirport(),
                r.getFlightArrAirport(),
                styles
        );
    }

    public void checkChatDto(ChatDto chatDto) {
        if (chatDto == null) {
            return;
        }
        checkUserInput(chatDto.getQuestion(), chatDto.getCurrent());
    }

    public void checkTransDto(TransDto dto) {
        if (dto == null) {
            return;
        }
        checkUserInput(dto.getFromCity(), dto.getToCity());
    }

    public void checkCityAndKeywords(String city, String keywords) {
        checkUserInput(city, keywords);
    }
}
