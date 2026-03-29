package com.jjl.mcpclient.controller;

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


    @PostMapping("upload")
    public LeeResult upload(@RequestParam("file") MultipartFile file){
        List<Document> documents = documentService.upload(file.getOriginalFilename(), file.getResource());
        return LeeResult.ok(documents);
    }

}
