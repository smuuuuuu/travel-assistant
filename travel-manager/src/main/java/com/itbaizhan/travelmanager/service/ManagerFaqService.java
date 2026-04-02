package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.pojo.ManagerTokenUsage;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import com.itbaizhan.travelmanager.event.FaqImportCompletedEvent;
import com.itbaizhan.travelmanager.mapper.TravelFaqMapper;
import com.itbaizhan.travelmanager.security.ManagerSecurityContext;
import com.itbaizhan.travelmanager.rag.KeyWordsEnricher;
import com.itbaizhan.travelmanager.rag.TextTokenSplit;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ManagerFaqService {

    private static final String BIZ_SCENE_FAQ_FILE_IMPORT = "FAQ_FILE_IMPORT";
    @Autowired
    private TravelFaqMapper travelFaqMapper;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private TextTokenSplit textTokenSplit;
    @Autowired
    private KeyWordsEnricher keyWordsEnricher;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private ManagerTokenUsageRecordService managerTokenUsageRecordService;

    private void insertListQdrant(List<TravelFaq> faqs) {
        List<Document> documents = new ArrayList<>();
        for (TravelFaq faq : faqs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", faq.getId());
            map.put("question", faq.getQuestion());
            map.put("answer", faq.getAnswer());
            if(StringUtils.hasText(faq.getCity())) {
                map.put("city", faq.getCity());
            }
            documents.add(new Document(faq.getId(), faq.getQuestion(), map));
        }
        vectorStore.add(documents);
    }

    public Page<TravelFaq> page(Integer page, Integer size){
        Page<TravelFaq> faqPage = new Page<>(page, size);
        QueryWrapper<TravelFaq> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("use_count");
        return travelFaqMapper.selectPage(faqPage,wrapper);
    }

    public List<TravelFaq> search(String query,String city,Integer k){
        SearchRequest searchRequest = null;
        if(StringUtils.hasText(query) && !StringUtils.hasText(city)){
            searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(k)
                    .build();
        }else {
            Filter.Expression filterExpression = new Filter.Expression(
                    Filter.ExpressionType.EQ,
                    new Filter.Key("city"),
                    new Filter.Value(city)
            );
            if(StringUtils.hasText(query)){
                searchRequest = SearchRequest.builder()
                        .query(query)
                        .filterExpression(filterExpression)
                        .topK(k)
                        .build();
            }else {
                searchRequest = SearchRequest.builder()
                        .filterExpression(filterExpression)
                        .topK(k)
                        .build();
            }
        }

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        List<String> list = documents.stream().map(Document::getId).toList();
        return travelFaqMapper.selectByIds(list);
    }

    @Transactional
    public void add(TravelFaq travelFaq){
        String id = UUID.randomUUID().toString();
        travelFaq.setId(id);
        travelFaqMapper.insert(travelFaq);
        if(General.ENABLE.equals(travelFaq.getStatus())){
            insertListQdrant(List.of(travelFaq));
        }
    }
    @Transactional
    public void update(TravelFaq travelFaq){
        travelFaqMapper.updateById(travelFaq);
        vectorStore.delete(List.of(travelFaq.getId()));
        if(General.ENABLE.equals(travelFaq.getStatus())){
            insertListQdrant(List.of(travelFaq));
        }
    }
    @Transactional
     public void delete(List<String> ids){
        travelFaqMapper.deleteByIds(ids);
        vectorStore.delete(ids);
     }
    @Transactional
    public void status(List<String> ids,Integer status){
        travelFaqMapper.status(status,ids);
        if(General.ENABLE.equals(status)){
            List<TravelFaq> faqs = travelFaqMapper.selectByIds(ids);
            insertListQdrant(faqs);
        }else {
            vectorStore.delete(ids);
        }
    }
    @Transactional
    public void upload(MultipartFile[] files){
        Long managerId = ManagerSecurityContext.currentManagerId();
        String managerName = ManagerSecurityContext.currentManagerName();
        List<Document> documents = new ArrayList<>();
        for (MultipartFile file : files) {
            Resource resource = file.getResource();
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
            documents.addAll(tikaDocumentReader.read());
        }
        processFileData(documents, managerId, managerName);
    }

    @Async
    @Transactional
    public void processFileData(List<Document> documents, Long managerId, String managerName){
        try {
            List<Document> splitCustomized = textTokenSplit.splitCustomized(documents);
            var enrichOutcome = keyWordsEnricher.enrichDocumentsWithUsage(splitCustomized);
            List<Document> enrichDocuments = enrichOutcome.documents();
            if (managerId != null && StringUtils.hasText(managerName)) {
                ManagerTokenUsage tokenUsage = new ManagerTokenUsage();
                tokenUsage.setManagerId(managerId);
                tokenUsage.setManagerName(managerName);
                tokenUsage.setBizScene(BIZ_SCENE_FAQ_FILE_IMPORT);
                tokenUsage.setModel(enrichOutcome.model());
                tokenUsage.setPromptTokens(enrichOutcome.promptTokens());
                tokenUsage.setCompletionTokens(enrichOutcome.completionTokens());
                tokenUsage.setTotalTokens(enrichOutcome.totalTokens());
                tokenUsage.setLlmCallCount(enrichOutcome.llmCallCount());
                tokenUsage.setDetail("faqRows=" + enrichDocuments.size());
                tokenUsage.setCreatedAt(LocalDateTime.now());
                managerTokenUsageRecordService.insertCommitted(tokenUsage);
            }
            for (Document enrichDocument : enrichDocuments) {
                TravelFaq travelFaq = new TravelFaq();
                travelFaq.setId(enrichDocument.getId());
                Map<String, Object> metadata = enrichDocument.getMetadata();
                travelFaq.setQuestion((String) metadata.get("keywords"));
                travelFaq.setAnswer(enrichDocument.getText());
                travelFaq.setCity((String) metadata.get("city"));
                travelFaq.setStatus(General.NO_ENABLE);
                travelFaq.setUseCount(0);
                travelFaqMapper.insert(travelFaq);
            }
            int n = enrichDocuments.size();
            applicationEventPublisher.publishEvent(new FaqImportCompletedEvent(
                    this,
                    managerName,
                    n,
                    true,
                    "FAQ 文件处理完成，共写入 " + n + " 条（待启用后入库向量）"));
        } catch (Exception e) {
            applicationEventPublisher.publishEvent(new FaqImportCompletedEvent(
                    this,
                    managerName,
                    0,
                    false,
                    "FAQ 文件处理失败：" + e.getMessage()));
            throw e;
        }
    }
}
