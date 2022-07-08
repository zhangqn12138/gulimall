package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundsTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 远程调用相关步骤（使用OpenFeign）：
     *    1.被调用的远程服务必须上线且放在注册中心中
     *    2.被调用的远程服务的主类上标注@EnableDiscoveryClient，开启服务注册和发现功能
     *    3.主动调用的服务必须上线且放在注册中心中
     *    4.主动调用的服务的主类上标注@EnableFeignClients(basePackages = "主动调用服务的接口所在包")（如果feign包在主类所在包下
     *    ，可以不用标注此注解就可以实现自动扫描，否则，必须要显式的声明）
     *    5.在主动调用的服务中创建接口（一般放在feign包下，好区分），在接口上用注解@FeignClient("被调用的服务名")声明被调用的服务
     *    6.在接口中的方法中，用@RequestMapping("被调用的服务的方法的url路径")声明被调用的具体路径
     *      接口中的方法不需要和被调用服务的url路径指向的方法名称一致，只要返回值一致，请求路径对应，且传输对象可以符合要求就可以
     *      （要求下面会细说）（有时为了方便，我们直接把被调用服务的方法复制黏贴过来）
     *
     * @param spuInfoVo
     */
    @Transactional
    @Override
    public void savespuInfo(SpuSaveVo spuInfoVo) {
        //1、保存spu基本信息：psm_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuInfoVo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //2、保存spu的描述图片：psm_spu_info_desc
        List<String> decript = spuInfoVo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getBrandId());
        spuInfoDescEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);

        //3、保存spu的图片集：psm_spu_images
        List<String> images = spuInfoVo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId() ,images);

        //4、保存spu的规格参数：psm_product_attr_value
        List<BaseAttrs> baseAttrs = spuInfoVo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map((baseAttr) -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setAttrId(baseAttr.getAttrId());
            AttrEntity attrEntity = attrService.getById(baseAttr.getAttrId());
            productAttrValueEntity.setAttrName(attrEntity.getAttrName());
            productAttrValueEntity.setAttrValue(baseAttr.getAttrValues());
            productAttrValueEntity.setQuickShow(baseAttr.getShowDesc());
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);

        //附：保存商品积分：gulimall_sms--->sms_spu_bounds
        Bounds bounds = spuInfoVo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(spuInfoEntity.getId());
        R rspu = couponFeignService.saveSpuBounds(spuBoundsTo);
        if(rspu.getCode() != 0){
            log.error("远程保存spu积分信息失败");
        }

        //5、保存当前spu对应的所有sku信息：
        List<Skus> skus = spuInfoVo.getSkus();
        //如果要是用流对skus里面的每个sku进行封装返回list再批量保存，那么由于下面的信息保存要和skuid进行关联，还要再取skuid
        //就会比较麻烦，所以不如用foreach()
        if(skus != null && skus.size() > 0){
            skus.forEach((sku) -> {
                //找到每个sku的默认图片
                String defaultImage = "";
                for (Images image : sku.getImages()) {
                    if(image.getDefaultImg() == 1){
                        defaultImage = image.getImgUrl();
                    }
                }

                //5.1 sku的基本信息：pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImage);
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();

                //5.2 sku的图片信息：pms_sku_images
                List<SkuImagesEntity> imageEntities = sku.getImages().stream().map((image) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    //每个skuid的每个图片都要存一条数据，后面的defaultImage字段为0或1，判断是否是默认图片
                    skuImagesEntity.setImgUrl(image.getImgUrl());
                    skuImagesEntity.setDefaultImg(image.getDefaultImg());
                    return skuImagesEntity;
                }).filter((entity) -> {
                    //返回true就是需要，返回false就是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(imageEntities);

                //5.3 sku的销售属性信息：pms_sku_sale_attr_value
                List<Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map((a) -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //5.4 sku的优惠、满减等信息：gulimall_sms--->sms_sku_ladder、sms_sku_full_reduction、sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if(skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    R rsku = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(rsku.getCode() != 0){
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }


    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w) -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status", status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
       return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {

        //1.组装需要的数据
        //1.1 查出当前spuId对应的所有的sku信息，品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkuBySpuId(spuId);

        //⑤ =====||>
        //首先查询出当前spuId的所有spu属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListforspu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        //再从数据库中查询出可以用来检索的属性
        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);
        //最后对上面的结果进行过滤和对拷
        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs);
            return attrs;
        }).collect(Collectors.toList());

        //④ ====||>
        //得到当前spuId对应的所有的skuId
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R skusHasStock = wareFeignService.getSkusHasStock(skuIdList);
            stockMap = skusHasStock.getData(new TypeReference<List<SkuHasStockVo>>(){}).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {//库存出现异常时，要捕获异常，并继续执行后面的代码
            e.printStackTrace();
        }

        //1.2 封装每个sku的信息，得到所需要的该spuId对应的需要上架的所有sku商品
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, esModel);
            //① 属性名不一样的信息
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            //② 品牌和分类的相关信息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            CategoryEntity categroy = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(categroy.getName());
            //③ 设置热度评分
            esModel.setHotScore(0L);
            //④ 发送远程调用查询库存并设置，远程调用多次太费时，所以选择一次查出spuId下所有的skuId的库存，在外面查询一次即可 ====||>
            if(finalStockMap == null){
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            //⑤ 查询规格属性，由于规格属性取决于spu，所以放在外面查询一次就好 ====||>
            esModel.setAttrs(attrsList);
            return esModel;
        }).collect(Collectors.toList());

        //2. 将数据发送给ES进行保存，交给gulimall-search操作
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){//远程调用成功
            //修改数据库spu状态为已上架
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else{//远程调用失败
            //TODO 重复调用：接口幂等性问题
        }
    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {

        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);
        Long spuId = skuInfoEntity.getSpuId();
        SpuInfoEntity spuInfoEntity = getById(spuId);
        return spuInfoEntity;
    }

}