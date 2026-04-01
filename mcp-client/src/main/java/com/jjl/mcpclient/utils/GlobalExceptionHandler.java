
package com.jjl.mcpclient.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientRequestException.class)
    public String handleWebClientRequestException(WebClientRequestException e) {
        log.error("网络请求失败：{} {}", e.getMessage(), e);
        return "网络请求失败，请稍后重试：" +e.getMessage();
    }

    @ExceptionHandler(WebClientResponseException.class)
    public String handleWebClientResponseException(WebClientResponseException e) {
        log.error("服务器响应异常：{} {}", e.getStatusCode(), e.getMessage(), e);
        return "服务器响应异常：" + e.getStatusCode();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return "系统繁忙，请稍后重试";
    }
}