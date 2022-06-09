package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.google.common.reflect.MutableTypeToInstanceMap;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有的分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子的树形结构图
        //2.1找到所有的一级分类
        List<CategoryEntity> level1Meuns = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null? 0 : menu1.getSort()) - (menu2.getSort() == null? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Meuns;
    }

    /**
     * 逻辑删除步骤：
     *      1.application.yml中加入配置，配置全局的逻辑删除规则（可不写，有默认值）
     *      2.配置逻辑删除的组件（3.1.1后可以省略）
     *      3.在对应实体类的某个属性加上逻辑删除注解@TableLogic
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {

        //TODO 检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        paths.add(catelogId);
        findParentPath(catelogId, paths);
        Collections.reverse(paths);
        System.out.println("paths==================================" + paths);
        return paths.toArray(new Long[paths.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    //使用SpringCache之后的使用缓存方式
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"),
//            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//    }
//    )
    @CacheEvict(value = {"category"}, allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 1. 基本使用：@Cacheable注解标注在方法上，表明当前方法的结果需要缓存，如果缓存中有，则方法不调用，如果缓存中没有，则调用方法。
     *             可以通过设置其value属性，属性值为数组，来指明缓存的数据要放到那个名字的缓存中。
     * 2. 默认行为：
     *      ① 如果缓存有，方法不被调用。
     *      ② key自动生成，默认为缓存名字::SimpleKey[]（自动生成的key值）
     *      ③ 缓存的value值默认使用jdk序列化机制将序列化后的数据保存在Redis中
     *      ④ 默认ttl为-1，即永久有效
     * 3. 自定义：
     *      ① 指定生成缓存使用的key：通过该注解的key属性指定，该属性接收一个SPEL表达式，此时，key会被调整为缓存名字::自定义部分。
     *      ② 指定缓存数据的存活时间：在配置文件中指定ttl，单位为ms
     *      ③ 将数据转换为json格式存在缓存中：见config.MyRedissonConfig
     * @return
     */
    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("调用了方法");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }


    //优化前：查询多次数据库
    /**
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();
        //2.封装数据
        Map<String, List<Catelog2Vo>> map = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //2.1 得到每一个的一级分类，查询到该一级分类的所有二级分类，并进行封装
            List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            List<Catelog2Vo> catelog2Vos = null;
            if(categoryEntities != null){
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //2.1.1 为每一个二级分类找到其所有的三级分类，并进行封装
                    List<CategoryEntity> level3CategoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level2.getCatId()));
                    if(level3CategoryEntities != null){
                        List<Catelog2Vo.Catelog3Vo> catelog3List = level3CategoryEntities.stream().map(level3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(),level3.getCatId().toString(), level3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return map;
    }
    **/

    //第一次优化后：查询一次数据库
    /**
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //查询出所有的分类数据并保存
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> map = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //2.1 得到每一个的一级分类，查询到该一级分类的所有二级分类，并进行封装
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if(categoryEntities != null){
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //2.1.1 为每一个二级分类找到其所有的三级分类，并进行封装
                    List<CategoryEntity> level3CategoryEntities = getParent_cid(selectList, level2.getCatId());
                    if(level3CategoryEntities != null){
                        List<Catelog2Vo.Catelog3Vo> catelog3List = level3CategoryEntities.stream().map(level3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(),level3.getCatId().toString(), level3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return map;
    }
    **/

    //第二次优化后：加入缓存
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
    }

    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        //得到锁后，再去缓存进行确定，如果没有才查询
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if(!StringUtils.isEmpty(catalogJSON)){
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        //查询出所有的分类数据并保存
        System.out.println("查询数据库");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> map = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //2.1 得到每一个的一级分类，查询到该一级分类的所有二级分类，并进行封装
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //2.1.1 为每一个二级分类找到其所有的三级分类，并进行封装
                    List<CategoryEntity> level3CategoryEntities = getParent_cid(selectList, level2.getCatId());
                    if (level3CategoryEntities != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3List = level3CategoryEntities.stream().map(level3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //3. 将查出的数据转换为json放入缓存中，并返回查出的数据
        String jsonString = JSON.toJSONString(map);
        stringRedisTemplate.opsForValue().set("catalogJSON", jsonString);
        return map;
    }

    //本地锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {
        //加本地锁：springboot所有组件都是单实例的，所以，所有的线程都只能用同一个实例去查询，因此，是同一把锁，可以锁住
        synchronized (this) {
            return getDataFromDB();
        }
    }

    //分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedis() {

        //进阶二：UUID防误删
        String uuid = UUID.randomUUID().toString();
        //进阶一：过期时间
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if(lock){
            System.out.println("分布式锁获取成功");
            //进阶四：简单改良自动延续过期时间
            Map<String, List<Catelog2Vo>> dataFromDB = null;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                //进阶三：保障删除锁操作的原子性
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end;";
                Long lockResult = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDB;
        }else{
            System.out.println("获取锁失败，等待重试");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //自旋的方式重试
            //TODO 个人想法：感觉这个地方既然一次查询到了，之后的每次不需要都获取一次锁，应该可以改进
            return getCatalogJsonFromDBWithRedis();
        }
    }

    //框架版本分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissonLock (){
        //只要名字一样，锁就是一样的，锁的名字会涉及到锁的粒度，锁的粒度越细，就会越快
        RLock lock = redisson.getLock("catalogJSON-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDB = null;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }
        return dataFromDB;
    }

    //使用SpringCache之前的使用缓存方式
    //@Override
    public Map<String, List<Catelog2Vo>> getCatalogJsonBeforeSpring(){
        //缓存中存放的是对象转换成的json字符串，缓存中取出的json字符串需要转换成对象===>【序列化和反序列化】
        //1. 加入缓存逻辑，缓存中的数据是json字符串，json字符串是跨语言、跨平台兼容的
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if(StringUtils.isEmpty(catalogJSON)){
            //2. 缓存中没有，查询数据库
            System.out.println("缓存未命中，查询数据库");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedissonLock();
            return catalogJsonFromDB;
        }
        //2. 如果缓存中有，则转换为指定的对象并返回
        System.out.println("缓存命中，直接返回");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    //使用SpringCache之后的使用缓存方式
    @Cacheable(value = {"category"}, key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        System.out.println("查询了数据库");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> map = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //2.1 得到每一个的一级分类，查询到该一级分类的所有二级分类，并进行封装
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    //2.1.1 为每一个二级分类找到其所有的三级分类，并进行封装
                    List<CategoryEntity> level3CategoryEntities = getParent_cid(selectList, level2.getCatId());
                    if (level3CategoryEntities != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3List = level3CategoryEntities.stream().map(level3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3List);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return map;
    }


    private void findParentPath(Long catelogId, List<Long> paths){
        CategoryEntity byId = this.getById(catelogId);
        Long parentCid = byId.getParentCid();
        if (parentCid == 0) {
            return;
        }
        paths.add(parentCid);
        findParentPath(parentCid, paths);
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return root.getCatId() == categoryEntity.getParentCid();
        }).map(categoryEntity -> {
            //1.找到子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2.进行排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}