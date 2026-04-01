package com.jjl.mcpclient.service.impl;

import cn.hutool.json.JSONUtil;
import com.jjl.mcpclient.entity.SearXngResponse;
import com.jjl.mcpclient.entity.SearchResult;
import com.jjl.mcpclient.service.SearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 使用本地搜索引擎联网搜索
 * @author jjl
 * @date 2023/9/5
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Value("${internet.websearch.url}")
    private String SEARXNG_GURL;

    @Value("${internet.websearch.count}")
    private Integer count;

    @Resource
    private OkHttpClient okHttpClient;

    @Override
    public List<SearchResult> searXNG(String query) {

        // 构建URL
        HttpUrl url = HttpUrl.parse(SEARXNG_GURL)
                .newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .build();

        log.info("url:{}", url.url());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()){

            if (!response.isSuccessful()){
                log.error("请求失败:{}", response.code());
                throw new RuntimeException("请求失败: HTTP :"+ response.code());
            }

            if (response.body() != null) {
                String responseResult = response.body().string();

                SearXngResponse searXngResponse = JSONUtil.toBean(responseResult, SearXngResponse.class);

                return searXngResponse.getResults();
            }
            log.error("请求失败:{}", response.message());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }
}
