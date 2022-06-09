package com.atguigu.gullimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gullimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.assertj.core.data.Index;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.fields.ExcludeFieldEnumSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 测试存储数据到ES
     * 保存更新二合一
     */
    @Test
    void contextLoads() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");//指定存储到user索引
        indexRequest.id("1");//指定要存储的数据的id，不指定则自动生成
        User user = new User();
        user.setUserName("胡琮梅");
        user.setAge(26);
        user.setGender("女");
        String jsonString = JSON.toJSONString(user);//设置要存储的具体数据
        indexRequest.source(jsonString, XContentType.JSON);
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);//指定请求设置项，并利用客户端进行传送，传送完毕获得该操作的响应数据
        System.out.println(index);//提取有用的响应数据
    }

    @Data
    @ToString
    static class User{
        private String userName;
        private Integer age;
        private String gender;
    }

    /**
     * 测试ES复杂查询
     */
    @Test
    public void searchData() throws IOException {
        //1.创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //1.1 指定需要检索的索引
        searchRequest.indices("users");
        //1.2 构造检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("userName", "胡"));//QueryBuilders是QueryBuilder的工具类
        //1.3 构造聚合条件
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("userName").size(10);//AggregationBuilders是AggregationBuilder的工具类
        sourceBuilder.aggregation(ageAgg);
        System.out.println(sourceBuilder.toString());
        //1.4 封装
        searchRequest.source(sourceBuilder);

        //2.进行检索
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        //3.分析检索结果
        //3.1 获取查到的所有记录
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            searchHit.getIndex();
            searchHit.getId();
            String sourceAsString = searchHit.getSourceAsString();
            //封装记录到javaBean
            User user = JSON.parseObject(sourceAsString, User.class);
            System.out.println("user：" + user);
        }
        //3.2 获取分析信息
        Aggregations aggregations = searchResponse.getAggregations();
    }

}
