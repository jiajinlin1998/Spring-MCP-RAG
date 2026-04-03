package com.jjl.mcpclient.utils;


import cn.hutool.core.collection.CollectionUtil;
import com.jjl.mcpclient.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SSEService {

    //存储所有用户
    private static final Map<String, SseEmitter> sseClients = new java.util.concurrent.ConcurrentHashMap<>();
    // 优化：添加连接数量限制
    private static final int MAX_CONNECTIONS = 100;

    /*
    * 创建sse连接
     */
    public static SseEmitter connect(String userId){
        // 优化：检查连接数量限制
        if (sseClients.size() >= MAX_CONNECTIONS) {
            log.warn("SSE连接数量已达到上限: {}", MAX_CONNECTIONS);
            // 返回一个超时的SseEmitter
            return new SseEmitter(1000L);
        }

        //设置超时时间0 超过30报错
        SseEmitter sseEmitter = new SseEmitter(0L);


        //注册回调方法
       sseEmitter.onTimeout(timeOutCallBack(userId));//超时回调
       sseEmitter.onCompletion(completionBack(userId));//完成回调
       sseEmitter.onError(errorBack(userId));//错误回调

        sseClients.put(userId, sseEmitter);
        log.info("SSE连接成功，用户id为: {}, 当前连接数: {}", userId, sseClients.size());
        return sseEmitter;
    }

    /**
     * 真正的业务逻辑 单个消息
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

    /**
     * 真正的业务逻辑 群发
     */
    public static void sedMagToAll(String msg){

        if(CollectionUtil.isEmpty(sseClients)){
            return;
        }

        sseClients.forEach((userId, sseEmitter) -> {
                sendEmitterMsg(userId, msg, SSEMsgType.MESSAGE, sseEmitter);
            }
        );
    }

    /*
    * 发送sse消息
     */
    private static void sendEmitterMsg(String userId, String msg, SSEMsgType msgType,SseEmitter sseEmitter){
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .id(userId)
                .data(msg)
                .name(msgType.type);
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
        log.info("SSE连接被移除，被移除用户id为: {}, 当前连接数: {}", userId, sseClients.size());
    }

    // 优化：定期清理无效连接
    static {
        // 启动定时任务，每15分钟清理一次无效连接（进一步增加清理间隔，减少CPU占用）
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(15 * 60 * 1000); // 优化：进一步增加清理间隔
                    cleanInvalidConnections();
                } catch (InterruptedException e) {
                    log.error("清理无效连接时发生异常: {}", e.getMessage());
                    // 线程被中断，退出循环
                    break;
                }
            }
        });
        // 设置为守护线程，避免阻止应用程序关闭
        cleanupThread.setDaemon(true);
        cleanupThread.setName("SSE-Cleanup-Thread");
        cleanupThread.start();
    }

    /**
     * 清理无效连接
     */
    private static void cleanInvalidConnections() {
        if (sseClients.isEmpty()) {
            return;
        }
        
        int initialSize = sseClients.size();
        if (initialSize == 0) {
            return;
        }
        
        // 优化：减少日志输出，只在有无效连接时才记录
        AtomicInteger removedCount = new AtomicInteger(0);
        
        // 优化：限制每次清理的连接数量，避免一次性处理过多连接
        int maxCleanupPerRun = 50;
        AtomicInteger processedCount = new AtomicInteger(0);
        
        // 遍历所有连接，检查是否有效
        sseClients.entrySet().removeIf(entry -> {
            // 限制每次清理的连接数量
            if (processedCount.get() >= maxCleanupPerRun) {
                return false;
            }
            
            try {
                // 优化：使用更轻量的方式检查连接是否有效
                // 注意：SseEmitter没有直接的isValid方法，所以我们仍然需要尝试发送消息
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .id(entry.getKey())
                        .data("")
                        .name("ping");
                entry.getValue().send(event);
                processedCount.incrementAndGet();
                return false;
            } catch (Exception e) {
                // 连接无效，移除
                removedCount.incrementAndGet();
                processedCount.incrementAndGet();
                return true;
            }
        });
        
        // 优化：只在有连接被移除时才记录日志
        if (removedCount.get() > 0) {
            log.info("清理无效SSE连接完成，移除了 {} 个连接，当前连接数: {}", removedCount.get(), sseClients.size());
        }
    }
}
