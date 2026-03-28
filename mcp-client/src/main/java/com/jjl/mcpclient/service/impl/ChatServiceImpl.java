package com.jjl.mcpclient.service.impl;

import com.jjl.mcpclient.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatServiceImpl implements ChatService {


    private ChatClient chatClient;

    private String systemPrompt = """
                                    你是一个非常聪明的人工智能助手，能帮我做很多事，我给你取了一个名字，叫汤圆
                                   """;

    public ChatServiceImpl(ChatClient.Builder  chatClientBuilder) {
        this.chatClient = chatClientBuilder
                //.defaultSystem()
                .build();
    }

    @Override
    public String ChatTest(String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @Override
    public Flux<ChatResponse> streamChatTest(String msg) {
        return chatClient.prompt(msg).stream().chatResponse();
    }

    @Override
    public Flux<String> streamChatStringTest(String msg) {
        return chatClient.prompt(msg).stream().content();
    }
}
