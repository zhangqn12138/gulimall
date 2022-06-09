package com.atguigu.gullimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gullimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gullimall.search.constant.EsConstant;
import com.atguigu.gullimall.search.service.ProductSaveService;
import com.sun.org.apache.bcel.internal.generic.NEW;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.awt.ModalExclude;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author QingnanZhang
 * @creat 2022-06-04 21:04
 **/
@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService{

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        //es中建立索引product，同时建立好映射关系（用Kiabna建立，代码见product-mapping.txt），给es中保存数据
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel skuEsModel : skuEsModels) {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);//指明要存储到那个索引
            indexRequest.id(skuEsModel.getSkuImg().toString());//s中的id为商品的skuId
            String info = JSON.toJSONString(skuEsModel);
            indexRequest.source(info, XContentType.JSON);//指明存入的数据的具体信息
            bulkRequest.add(indexRequest);
        }
        //上架并获取上架结果
        //远程调用失败则抛出异常
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //是否上架出现错误，返回true代表有错误
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架完成：{}，返回数据：{}", collect, bulk.toString());
        return b;
    }
}
