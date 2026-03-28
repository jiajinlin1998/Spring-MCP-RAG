package com.jjl.mcpclient.controller;

import com.jjl.mcpclient.service.ChatService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("HOLLE")
public class HolleController {

    @Resource
    private ChatService chatService;

    @GetMapping("WORLD")
   public String word(){
       return "你好";
   }

    @GetMapping("chat")
    public String chat(String msg){
        return chatService.ChatTest(msg);
    }

    @GetMapping("streamChat")
    public Flux<ChatResponse> streamChat(String msg){
        return chatService.streamChatTest( msg);
    }

    @GetMapping("streamChatString")
    public Flux<String> streamChatString(String msg, HttpServletResponse response){
        response.setCharacterEncoding("UTF-8");
        return chatService.streamChatStringTest(msg);
    }
}
