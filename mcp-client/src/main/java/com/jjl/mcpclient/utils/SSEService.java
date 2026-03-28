package com.jjl.mcpclient.utils;


import cn.hutool.core.collection.CollectionUtil;
import com.jjl.mcpclient.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class SSEService {

    //存储所有用户
    private static final Map<String, SseEmitter> sseClients = new java.util.concurrent.ConcurrentHashMap<>();

    /*
    * 创建sse连接
     */
    public static SseEmitter connect(String userId){

        //设置超时时间0 超过30报错
        SseEmitter sseEmitter = new SseEmitter(0L);


        //注册回调方法
       sseEmitter.onTimeout(timeOutCallBack(userId));//超时回调
       sseEmitter.onCompletion(completionBack(userId));//完成回调
       sseEmitter.onError(errorBack(userId));//错误回调

        sseClients.put(userId, sseEmitter);
        log.info("SSE连接成功，用户id为: {}", userId);
        return sseEmitter;
    }

    /**
     * 真正的业务逻辑
     */
    public static void sedMag(String userId, String msg, SSEMsgType msgType){

        if(CollectionUtil.isEmpty(sseClients)){
            return;
        }
        if (sseClients.containsKey(userId)){
            SseEmitter sseEmitter = sseClients.get(userId);
            sendEmitterMsg(userId, msg, msgType, sseEmitter);
        }

    }

    /*
    * 发送sse消息
     */
    private static void sendEmitterMsg(String userId, String msg, SSEMsgType msgType,SseEmitter sseEmitter){
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .id(userId)
                .data(msg)
                .name(msgType.value);
        try {
            sseEmitter.send(event);
        } catch (Exception e) {
            log.error("SSE发送消息异常 {}:", e.getMessage());
            //删除用户连接
           remove(userId);
        }
    }

    public static Runnable timeOutCallBack(String userId){
        return () -> {
            log.info("SSE连接超时，被移除用户id为: {}", userId);
            //删除用户连接
            sseClients.remove(userId);
        };
    }

    public static Runnable completionBack(String userId){
        return () -> {
            log.info("SSE完成");
            //删除用户连接
            sseClients.remove(userId);
        };
    }

    public static Consumer<Throwable> errorBack(String userId){
      return throwable -> {
          log.error("SSE连接异常，被移除用户id为: {}", userId);
          //删除用户连接
          sseClients.remove(userId);
      };
    }

    //删除用户
    public static void remove(String userId){
        sseClients.remove(userId);
        log.info("SSE连接被移除，被移除用户id为: {}", userId);
    }
}
