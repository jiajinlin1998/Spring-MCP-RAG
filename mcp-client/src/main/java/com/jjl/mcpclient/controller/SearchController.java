package com.jjl.mcpclient.controller;

import com.jjl.mcpclient.entity.SearchResult;
import com.jjl.mcpclient.service.SearchService;
import com.jjl.mcpclient.utils.LeeResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("internet")
public class SearchController {

    @Resource
    private SearchService searchService;

    /**
     * 使用本地搜索引擎联网查询
     * @param query 问题
     * @return 文件列表
     */
    @GetMapping("search")
    public LeeResult search(@RequestParam("query") String query){
        List<SearchResult> searchResults = searchService.searXNG(query);
        return LeeResult.ok(searchResults);
    }


}
