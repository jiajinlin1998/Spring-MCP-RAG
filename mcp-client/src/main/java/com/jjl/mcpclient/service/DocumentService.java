package com.jjl.mcpclient.service;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public interface DocumentService {

    /**
     * 上传文件
     * @param originalFilename 文件名
     * @param resource 文件
     */
    List<Document> upload(String originalFilename, Resource resource);
}
