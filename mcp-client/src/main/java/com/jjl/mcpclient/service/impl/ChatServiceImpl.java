package com.jjl.mcpclient.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.jjl.mcpclient.entity.ChatEntity;
import com.jjl.mcpclient.entity.ChatResponseEntity;
import com.jjl.mcpclient.entity.SearchResult;
import com.jjl.mcpclient.enums.SSEMsgType;
import com.jjl.mcpclient.service.ChatService;
import com.jjl.mcpclient.service.SearchService;
import com.jjl.mcpclient.utils.SSEService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    @Resource
    private SearchService searchService;

    private String systemPrompt = "你是一个非常聪明的人工智能助手，能帮我做很多事，我给你取了一个名字，叫汤圆";

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

    private static final String searchPrompt = """
                                                    你是一个网龄20年的资深网络搜索助手，请根据互联网结果返回的上下文，并且结合用户的提问，生成并且输出专业的回答
                                                    【上下文】
                                                    {context}
                                                    【问题】
                                                    {question}
                                                    【输出】
                                                    如果没有查到，请回复：不知道。
                                                    如果查到，请回复具体的内容。
                                            """;


    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultSystem(systemPrompt)
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
/*
        try {
            // 正确：使用 chatResponse() 才能接收 工具调用 + 流式输出
            chatClient.prompt(message)
                    .stream()
                    .chatResponse()  // <--- 必须用这个！！！
                    .subscribe(
                            chatResponse -> {
                                try {
                                    // 检查是否有工具调用
                                    if (chatResponse.getResult().getOutput().getToolCalls() != null && !chatResponse.getResult().getOutput().getToolCalls().isEmpty()) {
                                        log.info("AI决定调用工具: {}", chatResponse.getResult().getOutput().getToolCalls());
                                        // 工具调用会由Spring AI自动处理，不需要手动处理
                                    }
                                    
                                    // 获取AI输出
                                    String content = chatResponse.getResult().getOutput().getText();

                                    if (content != null && !content.isEmpty()) {
                                        SSEService.sedMag(userId, content, SSEMsgType.ADD);
                                        log.info("用户: {}, 消息: {}", userId, content);
                                    }
                                } catch (Exception e) {
                                    log.error("处理聊天响应异常", e);
                                    SSEService.sedMag(userId, "处理响应时出错: " + e.getMessage(), SSEMsgType.MESSAGE);
                                }
                            },
                            error -> {
                                log.error("流式输出异常", error);
                                // 检查是否是JSON解析错误
                                if (error.getMessage().contains("No content to map due to end-of-input")) {
                                    SSEService.sedMag(userId, "工具调用成功！文件已创建。", SSEMsgType.MESSAGE);
                                } else {
                                    SSEService.sedMag(userId, "出错了: " + error.getMessage(), SSEMsgType.MESSAGE);
                                }
                            },
                            () -> {
                                // 结束
                                ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
                                SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
                            }
                    );
        } catch (Exception e) {
            log.error("启动聊天流异常", e);
            SSEService.sedMag(userId, "启动聊天时出错: " + e.getMessage(), SSEMsgType.MESSAGE);
            ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
            SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
        }*/
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

        try {
            //构建提示词
            String context = "";
            if (CollectionUtil.isNotEmpty(documentList)) {
                context = documentList.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));

            }

            //组装提示词
            Prompt prompt = new Prompt(ragPrompt.replace("{context}",context).replace("{question}", message));

            log.info("提示词: {}", prompt);

            // 正确流式写法：来一条发一条，不阻塞、不吃 CPU
            chatClient.prompt(prompt)
                    .stream()
                    .content()
                    .subscribe(
                            content -> {
                                // 来一条发一条
                                SSEService.sedMag(userId, content, SSEMsgType.ADD);
                                log.info("用户: {}, 消息: {}", userId, content);
                            },
                            error -> {
                                log.error("流式输出异常", error);
                                SSEService.sedMag(userId, "出错了: " + error.getMessage(), SSEMsgType.MESSAGE);
                            },
                            () -> {
                                // 全部发送完成
                                ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
                                SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
                            }
                    );
        } catch (Exception e) {
            log.error("处理知识搜索请求时发生异常", e);
            SSEService.sedMag(userId, "处理请求时出错: " + e.getMessage(), SSEMsgType.MESSAGE);
            ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
            SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
        }
    }

    @Override
    public void doInternetSearch(ChatEntity chatEntity) {

        String userId = chatEntity.getCurrentUserName();
        String message = chatEntity.getMessage();
        String botMsgId = chatEntity.getBotMsgId();

        try {
            List<SearchResult> searchResultList =  searchService.searXNG(message);
            String finalPrompt = buildSearchPrompt(message, searchResultList);

            //组装提示词
            Prompt prompt = new Prompt(finalPrompt);

            log.info("提示词: {}", prompt);

            // 正确流式写法：来一条发一条，不阻塞、不吃 CPU
            chatClient.prompt(prompt)
                    .stream()
                    .content()
                    .subscribe(
                            content -> {
                                // 来一条发一条
                                SSEService.sedMag(userId, content, SSEMsgType.ADD);
                                log.info("用户: {}, 消息: {}", userId, content);
                            },
                            error -> {
                                log.error("流式输出异常", error);
                                SSEService.sedMag(userId, "出错了: " + error.getMessage(), SSEMsgType.MESSAGE);
                            },
                            () -> {
                                // 全部发送完成
                                ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
                                SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
                            }
                    );
        } catch (Exception e) {
            log.error("处理联网搜索请求时发生异常", e);
            SSEService.sedMag(userId, "处理请求时出错: " + e.getMessage(), SSEMsgType.MESSAGE);
            ChatResponseEntity entity = new ChatResponseEntity("", botMsgId);
            SSEService.sedMag(userId, JSONUtil.toJsonStr(entity), SSEMsgType.FINISH);
        }
    }

    private static String buildSearchPrompt(String question, List<SearchResult> searchResultList) {

        StringBuilder context = new StringBuilder();

        searchResultList.forEach(searchResult -> {
            context.append(
                    String.format("<context>\n[来源] %s \n [摘要] %s \n</context>\n",
                            searchResult.getUrl(),
                            searchResult.getContent()));
        });

        return searchPrompt.replace("{context}", context).replace("{question}", question);
    }

}
