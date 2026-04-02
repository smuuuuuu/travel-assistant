package com.itbaizhan.travelcommon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itbaizhan.travelcommon.pojo.TravelFaq;

public interface AiAssistantFaqService {

    void createFaq(TravelFaq faq);

   /* IPage<TravelFaq> searchFaq(int page,int size,Integer type);

    TravelFaq getFaqById(String id);

    void updateFaq(TravelFaq faq);

    void deleteFaq(String id);

    void mysqlToQdrant();*/

    boolean existFaq(String question);

    TravelFaq searchBestAnswer(String question);
}
