package com.jjl.mcpclient.enums;

/**
 * SSE消息类型枚举
 */
public enum SSEMsgType {

    MESSAGE("message","单词发送的普通类型消息"),
    ADD("add","消息追加，使用于流式stream推送"),
    FINISH("finish","消息完成"),
    CUSTOM_EVENT("customEvent","自定义事件"),
    DONE("done","完成");

    public final String type;
    public final String value;

    SSEMsgType (String type, String value) {
        this.type = type;
        this.value = value;
    }
}
