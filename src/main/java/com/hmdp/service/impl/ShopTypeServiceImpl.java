package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getShopTypeList() {

        String key = SHOP_TYPE_KEY;

        // 1.从redis中查询商铺类型缓存
        List<String> shopTypeList = new ArrayList<>();
        shopTypeList = stringRedisTemplate.opsForList().range(key, 0, -1);

        // 2.若存在，则直接返回数据
        if (!shopTypeList.isEmpty()) {
            ArrayList<ShopType> typeList = new ArrayList<>();
            for (String shopType: shopTypeList) {
                ShopType type = JSONUtil.toBean(shopType, ShopType.class);
                typeList.add(type);
            }
            return Result.ok(typeList);
        }

        // 3.不存在，从数据库中查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        // 4.仍不存在，报错
        if (shopTypes.isEmpty()) {
            Result.fail("该分类不存在");
        }

        // 5.存在，写入Redis并返回数据
        for (ShopType type: shopTypes) {
            String s = JSONUtil.toJsonStr(type);
            shopTypeList.add(s);
        }

        stringRedisTemplate.opsForList().rightPushAll(key, shopTypeList);
        return Result.ok(shopTypes);
    }
}
