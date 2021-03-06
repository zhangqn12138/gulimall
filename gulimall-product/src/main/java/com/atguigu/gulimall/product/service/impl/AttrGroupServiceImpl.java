package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        System.out.println("key========================" + key);
        //select * from pms_attr_group where catelog_id = ? and (attr_group_id = key or attr_group_name like %key%)
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if(catelogId == 0){
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),//????????????
                    wrapper//????????????
            );
            return new PageUtils(page);
        }else{
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),//????????????
                    wrapper//????????????
            );
            return new PageUtils(page);
        }
    }

    /**
     * ????????????id????????????????????????????????????
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> gerAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //1?????????????????????
        List<AttrGroupEntity> groups = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //2?????????????????????
        List<AttrGroupWithAttrsVo> attrsVo = groups.stream().map((group) -> {
            //????????????
            AttrGroupWithAttrsVo groupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, groupWithAttrsVo);
            //????????????
            List<AttrEntity> attrs = attrService.getRelationAttr(group.getAttrGroupId());
            groupWithAttrsVo.setAttrs(attrs);
            return groupWithAttrsVo;
        }).collect(Collectors.toList());
        return attrsVo;
    }

    @Override
    public List<SpuItemAttrGroupVo> gerAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        //1.????????????spu????????????????????????????????????????????????????????????????????????????????????
        //1.1 ??????????????????id???pms_attr_group???????????????????????????
        //1.2 ????????????id???pms_attr_attrgroup_relation??????????????????id
        //1.3 ????????????id???pms_attr??????????????????
        //1.4 ????????????id???spuid???pms_product_attr_value???????????????????????????
        List<SpuItemAttrGroupVo> vos = baseMapper.gerAttrGroupWithAttrsBySpuId(spuId, catalogId);
        return vos;
    }

}