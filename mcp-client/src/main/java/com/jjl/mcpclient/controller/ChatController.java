package com.jjl.mcpclient.controller;

import com.jjl.mcpclient.entity.ChatEntity;
import com.jjl.mcpclient.service.ChatService;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("chat")
public class ChatController {

    @Resource
    private ChatService chatService;


    @PostMapping("doChat")
    public void doChat(@RequestBody ChatEntity chatEntity){
         chatService.doChat(chatEntity);
    }

}
