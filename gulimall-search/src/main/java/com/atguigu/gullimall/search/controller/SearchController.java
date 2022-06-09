package com.atguigu.gullimall.search.controller;

import com.atguigu.gullimall.search.service.MallSearchService;
import com.atguigu.gullimall.search.vo.SearchParam;
import com.atguigu.gullimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;


/**
 * @author QingnanZhang
 * @creat 2022-06-08 10:27
 **/
@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request/*此参数面包屑使用*/){
        searchParam.set_queryString(request.getQueryString());//此语句面包屑使用
        //1.根据传递来的页面的查询信息，去es中检索商品
        SearchResult result = mallSearchService.search(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
