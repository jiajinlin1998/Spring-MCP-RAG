package com.jjl.mcpclient.service;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

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
}
