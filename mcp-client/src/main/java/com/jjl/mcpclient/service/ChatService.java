package com.jjl.mcpclient.service;

import com.jjl.mcpclient.entity.ChatEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    /**
     * 测试聊天
     * @param msg 消息
     * @return 回复
     */
    public String ChatTest(String msg);

    /**
     * 流式聊天
     * @param msg 消息
     * @return 回复
     */
    public Flux<ChatResponse> streamChatTest(String msg);

    /**
     * 流式聊天
     * @param msg 消息
     * @return 回复
     */
    public Flux<String> streamChatStringTest(String msg);

    /**
     * 测试聊天
     * @param chatEntity 大模型交互
     */
    void doChat(ChatEntity chatEntity);

    /**
     * RAG知识库检索给大模型进行输出
     * @param documents 文档
     * @param chatEntity 大模型交互
     */
    void doChatSearch(List<Document> documents, ChatEntity chatEntity);


    /**
     * 联网搜索
     * @param chatEntity 大模型交互
     */
    void doInternetSearch(ChatEntity chatEntity);
}
