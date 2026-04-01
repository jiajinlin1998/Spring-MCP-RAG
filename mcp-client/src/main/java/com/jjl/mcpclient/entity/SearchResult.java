package com.jjl.mcpclient.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SearchResult {

    public String title;
    public String content;
    public String url;
    public Double score;
}
