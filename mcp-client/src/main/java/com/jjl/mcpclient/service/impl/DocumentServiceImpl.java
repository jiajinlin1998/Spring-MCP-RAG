package com.jjl.mcpclient.service.impl;

import com.jjl.mcpclient.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    @Override
    public List<Document> upload(String originalFilename, Resource resource) {
        // 使用 TikaDocumentReader 代替 TextReader，支持多种文件格式
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        List<Document> documents = tikaReader.get();
        
        // 为每个文档添加文件名元数据
        for (Document document : documents) {
            document.getMetadata().put("filename", originalFilename);
        }
        
        log.info("文档: {}", documents);
        return documents;
    }
}
