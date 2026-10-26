package com.huaqiu.xiaohashu.note.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Meilisearch 客户端配置
 *
 * 通过 meili.url 和 meili.key 连接已部署的 Meilisearch 服务，
 * 供 NoteSearchService 做笔记索引同步与全文搜索。
 */
@SuppressWarnings("SpellCheckingInspection")
@Configuration
public class MeiliConfig {

    @Value("${meili.url}")
    private String url;

    @Value("${meili.key}")
    private String key;

    @Bean
    public Client meiliClient() {
        return new Client(new Config(url, key));
    }

}
