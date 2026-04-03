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

        // 优化：将切割好的文档存储到 redis 向量库中，分批处理避免 batch size 超限
        // 使用线程池处理，避免创建过多线程
        int batchSize = 10; // 优化：增加批处理大小，减少网络请求次数
        // 创建线程池，限制线程数量
        java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(5);
        try {
            for (int i = 0; i < apply.size(); i += batchSize) {
                final int finalI = i;
                final int finalEnd = Math.min(i + batchSize, apply.size());
                executorService.submit(() -> {
                    try {
                        List<Document> batch = apply.subList(finalI, finalEnd);
                        redisVectorStore.add(batch);
                        log.info("已处理批次，大小: {}", batch.size());
                    } catch (Exception e) {
                        log.error("存储文档到 Redis 时发生异常: {}", e.getMessage());
                    }
                });
            }
        } finally {
            // 关闭线程池
            executorService.shutdown();
            try {
                // 等待所有任务完成
                if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 优化：减少日志输出，避免大量日志导致性能问题
        log.info("分块数量: {}", apply.size());
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
