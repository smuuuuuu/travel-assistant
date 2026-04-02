package com.itbaizhan.travel_ai_service.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.itbaizhan.travel_ai_service.mapper.TravelFaqMapper;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import com.itbaizhan.travelcommon.service.AiAssistantFaqService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@DubboService
public class FaqServiceImpl implements AiAssistantFaqService {
    @Autowired
    private VectorStore store;
    @Autowired
    private TravelFaqMapper faqMapper;

    private void insertQdrant(TravelFaq faq) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", faq.getId());
        map.put("question", faq.getQuestion());
        map.put("answer", faq.getAnswer());
        if(StrUtil.isNotBlank(faq.getCity())){
            map.put("city", faq.getCity());
        }
        Document document = new Document(faq.getId(), faq.getQuestion(), map);
        List<Document> documents = List.of(document);
        store.add(documents);
    }

    @Override
    public void createFaq(TravelFaq faq) {
        if(faq.getId() == null){
            faq.setId(UUID.randomUUID().toString());
        }
        if(faq.getStatus() == null){
            faq.setStatus(1);
        }
        if(faq.getUseCount() == null){
            faq.setUseCount(0);
        }
        if(faq.getStatus() == 1){
            insertQdrant(faq);
        }
        faqMapper.insert(faq);
    }

    /*@Override
    public IPage<TravelFaq> searchFaq(int page, int size, Integer type) {
        Page<TravelFaq> faqIPage = new Page<>(page, size);
        QueryWrapper<TravelFaq> wrapper = new QueryWrapper<>();
        if(type != null){
            wrapper.eq("type", type);
        }
        return faqMapper.selectPage(faqIPage, wrapper);
    }

    @Override
    public TravelFaq getFaqById(String id) {
        return faqMapper.selectById(id);
    }

    @Override
    public void updateFaq(TravelFaq faq) {
        faqMapper.updateById(faq);
        if(faq.getStatus() != null && faq.getStatus() == 0){
            deleteFaq(faq.getId());
        }else {
            deleteFaq(faq.getId());
            insertQdrant(faq);
        }
    }

    @Override
    public void deleteFaq(String id) {
        faqMapper.deleteById(id);
        deleteFaQdrant(id);
    }

    @Override
    public void mysqlToQdrant() {
        QueryWrapper<TravelFaq> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        List<TravelFaq> faqs = faqMapper.selectList(wrapper);
        for (TravelFaq faq : faqs) {
            insertQdrant(faq);
        }
    }

    private void deleteFaQdrant(String id) {
        List<String> id1 = List.of(id);
        store.delete(id1);
    }*/
    @Override
    public boolean existFaq(String question) {
        SearchRequest searchRequest = SearchRequest.builder().query(question).topK(1).build();
        List<Document> documents = store.similaritySearch(searchRequest);
        return documents != null && !documents.isEmpty();
    }

    @Override
    public TravelFaq searchBestAnswer(String question) {
        TravelFaq travelFaq = findByVectorSearch(question);
        if(travelFaq != null){
            increaseUseCount(travelFaq.getId());
        }
        return travelFaq;
    }
    private TravelFaq findByVectorSearch(String question) {
        SearchRequest searchRequest = SearchRequest.builder().query(question).topK(1).build();
        List<Document> documents = store.similaritySearch(searchRequest);
        if (documents != null && !documents.isEmpty()) {
            Document doc = documents.get(0);
            String faqId = doc.getId();
            return faqMapper.selectById(faqId);
        }
        return null;
    }
    private void increaseUseCount(String id){
        TravelFaq faq = faqMapper.selectById(id);
        if(faq != null){
            faq.setUseCount(faq.getUseCount() + 1);
            faqMapper.updateById(faq);
        }
    }

    /**
     * 批量增加向量检索命中的 FAQ 使用次数
     */
    public void increaseUseCountBatch(List<String> ids){
        if(ids == null || ids.isEmpty()){
            return;
        }
        UpdateWrapper<TravelFaq> wrapper = new UpdateWrapper<>();
        wrapper.in("id", ids).setSql("use_count = use_count + 1");
        faqMapper.update(null, wrapper);
    }
}
