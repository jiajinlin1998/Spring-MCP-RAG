package com.jjl.mcpclient;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.ai.model.transformers.autoconfigure.TransformersEmbeddingModelAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        TransformersEmbeddingModelAutoConfiguration.class
})
public class McpClientApplication {

    public static void main(String[] args) {
        //读取.env文件
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        //将.env文件中的变量设置为系统变量
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(),entry.getValue());
        });

        SpringApplication.run(McpClientApplication.class, args);
    }

}
