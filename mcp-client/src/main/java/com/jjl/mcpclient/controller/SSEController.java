package com.jjl.mcpclient.controller;

import com.jjl.mcpclient.enums.SSEMsgType;
import com.jjl.mcpclient.service.ChatService;
import com.jjl.mcpclient.utils.SSEService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.awt.*;

@RestController
@RequestMapping("SSE")
public class SSEController {

    /** 前端发送请求连接 链接sse服务
     * 创建sse连接
     * @param userId
     * @return
     */
   @GetMapping(path = "connect", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
   public SseEmitter connect(@RequestParam String userId){
        return SSEService.connect(userId);
   }

   /** SSE发送单个消息
    * @param userId
    * @param msg
    * @return
    */
    @GetMapping("sedMsg")
    public Object sedMag(@RequestParam String userId,@RequestParam String msg){
        SSEService.sedMag(userId, msg, SSEMsgType.MESSAGE);
        return "OK";
    }

    /** SSE发送群发消息
     * @param
     * @return
     */
    @GetMapping("sedMagALL")
    public Object sedMagALL(@RequestParam String msg){
        SSEService.sedMagToAll(msg);
        return "OK";
    }

}
