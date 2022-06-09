package com.atguigu.gullimall.search.service;

import com.atguigu.gullimall.search.vo.SearchParam;
import com.atguigu.gullimall.search.vo.SearchResult;
import org.springframework.stereotype.Service;

/**
 * @author QingnanZhang
 * @creat 2022-06-08 11:27
 **/
@Service
public interface MallSearchService {
    /**
     *
     * @param searchParam 检索的所有参数
     * @return 检索结果，包含页面需要的所有信息
     */
    SearchResult search(SearchParam searchParam);
}
