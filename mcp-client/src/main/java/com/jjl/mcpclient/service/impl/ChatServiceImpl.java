package com.jjl.mcpclient.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.jjl.mcpclient.entity.ChatEntity;
import com.jjl.mcpclient.entity.ChatResponseEntity;
import com.jjl.mcpclient.enums.SSEMsgType;
import com.jjl.mcpclient.service.ChatService;
import com.jjl.mcpclient.utils.SSEService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {


    private ChatClient chatClient;

    private String systemPrompt = """
                                    你是一个非常聪明的人工智能助手，能帮我做很多事，我给你取了一个名字，叫汤圆
                                   """;

    private static final String ragPrompt = """
                                                    基于上下文的知识库内容回答问题：
                                                    【上下文】
                                                    {context}
                                                    【问题】
                                                    {question}
                                                    【输出】
                                                    如果没有查到，请回复：不知道。
                                                    如果查到，请回复具体的内容。不相关的近似内容不必提到。
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

    @Override
    public void doChat(ChatEntity chatEntity) {
        String userId = chatEntity.getCurrentUserName();
        String message = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        Flux<String> stream = chatClient.prompt(message).stream().content();
        List<String> list = stream.toStream().map(chatResPonse -> {
            SSEService.sedMag(userId, chatResPonse, SSEMsgType.ADD);
            log.info("用户: {}, 消息: {}", userId, chatResPonse);
            return chatResPonse;
        }).toList();

        String collect = list.stream().collect(Collectors.joining());
        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(collect, botMsgId);

        SSEService.sedMag(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

    }


    @Override
    public void doChatSearch(List<Document> documentList, ChatEntity chatEntity) {
        String userId = chatEntity.getCurrentUserName();
        String message = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        //构建提示词
        String context = null;
        if (CollectionUtil.isNotEmpty(documentList)) {
            context = documentList.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

        }

        //组装提示词
        Prompt prompt = new Prompt(ragPrompt.replace("{context}",context).replace("{question}", message));

        log.info("提示词: {}", prompt);

        Flux<String> stream = chatClient.prompt(prompt).stream().content();
        List<String> list = stream.toStream().map(chatResPonse -> {
            SSEService.sedMag(userId, chatResPonse, SSEMsgType.ADD);
            log.info("用户: {}, 消息: {}", userId, chatResPonse);
            return chatResPonse;
        }).toList();

        String collect = list.stream().collect(Collectors.joining());
        ChatResponseEntity chatResponseEntity = new ChatResponseEntity(collect, botMsgId);

        SSEService.sedMag(userId, JSONUtil.toJsonStr(chatResponseEntity), SSEMsgType.FINISH);

    }

}
