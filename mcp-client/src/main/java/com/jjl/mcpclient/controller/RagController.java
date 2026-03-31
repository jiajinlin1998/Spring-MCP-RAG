package com.jjl.mcpclient.controller;

import com.jjl.mcpclient.entity.ChatEntity;
import com.jjl.mcpclient.service.ChatService;
import com.jjl.mcpclient.service.DocumentService;
import com.jjl.mcpclient.utils.LeeResult;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @Resource
    private ChatService chatService;

    /**
     * 上传文件
     * @param file 文件
     * @return 文件列表
     */
    @PostMapping("upload")
    public LeeResult upload(@RequestParam("file") MultipartFile file){
        List<Document> documents = documentService.upload(file.getOriginalFilename(), file.getResource());
        return LeeResult.ok(documents);
    }

    /*
     * 浏览器简单模拟用户提问
     */
    @GetMapping("select")
    public LeeResult select(@RequestParam String question){
        List<Document> documents = documentService.select(question);
        return LeeResult.ok(documents);
    }

    /**
     * 搜索本地知识库
     * @return 文件列表
     */
    @PostMapping("search")
    public LeeResult search(@RequestBody ChatEntity chatEntity){
        List<Document> documents = documentService.search(chatEntity.getMessage());
        chatService.doChatSearch(documents, chatEntity);
        return LeeResult.ok(documents);
    }

}
