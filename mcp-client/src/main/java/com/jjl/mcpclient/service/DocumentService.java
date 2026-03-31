package com.jjl.mcpclient.service;

import com.jjl.mcpclient.entity.ChatEntity;
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

    /*
     * 浏览器简单模拟用户提问
     */
    List<Document> select(String question);

    /**
     * 前端联网搜索功能
     * @return 文件列表
     */
    List<Document> search(String question);
}
