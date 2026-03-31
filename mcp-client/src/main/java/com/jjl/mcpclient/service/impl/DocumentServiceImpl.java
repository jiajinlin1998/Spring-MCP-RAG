package com.jjl.mcpclient.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.jjl.mcpclient.entity.ChatEntity;
import com.jjl.mcpclient.entity.ChatResponseEntity;
import com.jjl.mcpclient.enums.SSEMsgType;
import com.jjl.mcpclient.service.DocumentService;
import com.jjl.mcpclient.utils.CustomTextSplitter;
import com.jjl.mcpclient.utils.SSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {



    private final RedisVectorStore redisVectorStore;

    private ChatClient chatClient;

    @Override
    public List<Document> upload(String originalFilename, Resource resource) {
        // 使用 TikaDocumentReader 代替 TextReader，支持多种文件格式
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        List<Document> documents = tikaReader.get();
        
        // 为每个文档添加文件名元数据
        for (Document document : documents) {
            document.getMetadata().put("filename", originalFilename);
        }

        //默认的文本切割器
     /*   TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> apply = splitter.apply(documents);*/
        CustomTextSplitter splitter = new CustomTextSplitter();
        List<Document> apply = splitter.apply(documents);

        //将切割好的文档存储到 redis 向量库中，分批处理避免 batch size 超限
        int batchSize = 5;
        for (int i = 0; i < apply.size(); i += batchSize) {
            int end = Math.min(i + batchSize, apply.size());
            List<Document> batch = apply.subList(i, end);
            redisVectorStore.add(batch);
            log.info("已处理批次 {}/{}，共 {} 个文档", (i / batchSize) + 1, (apply.size() + batchSize - 1) / batchSize, batch.size());
        }

        log.info("分块: {}", apply);
        //log.info("文档: {}", documents);
        return documents;
    }

    @Override
    public List<Document> select(String question) {
        return redisVectorStore.similaritySearch(question);
    }

    @Override
    public List<Document> search(String question) {
        return redisVectorStore.similaritySearch(question);
    }

}
