package com.hmall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.HMallApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 5种基本数据类型测试类
 * 演示实际应用场景和使用方法
 * 
 * 5种基本数据类型：
 * 1. String - 字符串
 * 2. Hash - 哈希表
 * 3. List - 列表
 * 4. Set - 集合
 * 5. ZSet - 有序集合
 */
@SpringBootTest(classes = HMallApplication.class)
public class RedisDataTypesTest {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 清理测试数据
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    // ==================== String 类型测试 ====================
    
    /**
     * String类型 - 基本操作
     * 应用场景：缓存、计数器、分布式锁、session存储
     */
    @Test
    void testStringBasic() {
        // 1. 基本的set/get操作 - 用户session缓存
        // 这行代码的作用是：将键为"session:user:1001"，值为"张三"的数据存入Redis，并设置该键值对30分钟后过期。常用于用户session信息的缓存。
        redisTemplate.opsForValue().set("session:user:1001", "张三", Duration.ofMinutes(30));
        String username = redisTemplate.opsForValue().get("session:user:1001");
        // 断言从Redis中获取到的用户名应该等于"张三"
        assertEquals("张三", username);
        
        // 2. 分布式锁场景
        String lockKey = "lock:order:1001";
        // 这行代码的作用是：尝试以原子方式为key "lock:order:1001" 设置值为"locked"，并设置30秒的过期时间。
        // 如果该key不存在，则设置成功并返回true，表示成功获取分布式锁；如果已存在，则返回false，表示锁已被其他线程占用。
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));
        assertTrue(lockAcquired, "应该成功获取锁");
        
        // 再次尝试获取锁应该失败
        Boolean lockFailed = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));
        assertFalse(lockFailed, "不应该重复获取锁");
        
        // 释放锁
        redisTemplate.delete(lockKey);
    }

    /**
     * String类型 - 计数器应用
     * 应用场景：页面访问量、商品浏览次数、点赞数、库存扣减
     */
    @Test
    void testStringCounter() {
        // 1. 页面访问量统计
        String pageViewKey = "page:views:home";
        
        // 模拟10次页面访问
        for (int i = 0; i < 10; i++) {
            // 这行代码的作用是：对Redis中key为pageViewKey的值进行自增操作，每调用一次，该key对应的数值加1，常用于统计页面访问量等计数场景。
            redisTemplate.opsForValue().increment(pageViewKey);
        }
        
        // 这行代码的作用是：从Redis中获取key为pageViewKey的值，并将其转换为字符串类型。
        String views = redisTemplate.opsForValue().get(pageViewKey);
        // 断言从Redis中获取到的页面访问量应该等于10
        assertEquals("10", views);
        
        // 2. 商品点赞数
        String likeKey = "item:likes:1001";
        // 这行代码的作用是：对Redis中key为likeKey的值进行原子性自增操作，每次调用增加5个赞，常用于统计商品点赞数等场景。
        redisTemplate.opsForValue().increment(likeKey, 5); // 一次性增加5个赞
        // 这行代码的作用是：从Redis中获取key为likeKey的值，并将其转换为字符串类型。
        String likes = redisTemplate.opsForValue().get(likeKey);
        // 断言从Redis中获取到的点赞数应该等于5
        assertEquals("5", likes);
        
        // 3. 库存扣减（原子操作）
        String stockKey = "item:stock:1001";
        // 这行代码的作用是：将key为stockKey的值设置为"100"，常用于初始化库存等场景。
        redisTemplate.opsForValue().set(stockKey, "100"); // 初始库存100
        
        // 扣减库存
        // 这行代码的作用是：对Redis中key为stockKey的值进行原子性递减操作，将其值减少3，常用于库存扣减等场景。返回值为递减后的库存数量。
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey, 3);
        assertEquals(97L, remainingStock);
    }

    /**
     * String类型 - 缓存复杂对象
     * 应用场景：用户信息缓存、商品详情缓存、配置信息缓存
     */
    @Test
    void testStringObjectCache() throws JsonProcessingException {
        // 模拟商品信息
        Map<String, Object> product = new HashMap<>();
        product.put("id", 1001);
        product.put("name", "iPhone 15 Pro");
        product.put("price", 7999.00);
        product.put("stock", 50);
        product.put("category", "手机");
        
        // 序列化为JSON存储到Redis
        String productJson = objectMapper.writeValueAsString(product);
        String cacheKey = "product:detail:1001";
        // 这行代码的作用是：将序列化后的商品信息（JSON字符串）以cacheKey为键存入Redis，并设置过期时间为1小时，常用于缓存商品详情等场景。
        redisTemplate.opsForValue().set(cacheKey, productJson, Duration.ofHours(1));
        
        // 从缓存获取商品信息
        String cachedProductJson = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedProductJson);
        
        // 反序列化验证
        Map<String, Object> cachedProduct = objectMapper.readValue(cachedProductJson, Map.class);
        assertEquals(1001, cachedProduct.get("id"));
        assertEquals("iPhone 15 Pro", cachedProduct.get("name"));
        assertEquals(7999.0, cachedProduct.get("price"));
    }

    // ==================== Hash 类型测试 ====================
    
    /**
     * Hash类型 - 用户信息存储
     * 应用场景：用户信息、商品属性、配置项管理
     * 优势：可以单独操作对象的某个字段，比String存储JSON更灵活
     */
    @Test
    void testHashUserInfo() {
        String userKey = "user:info:1001";
        
        // 存储用户信息的各个字段
        redisTemplate.opsForHash().put(userKey, "id", "1001");
        redisTemplate.opsForHash().put(userKey, "name", "李四");
        redisTemplate.opsForHash().put(userKey, "email", "lisi@example.com");
        redisTemplate.opsForHash().put(userKey, "age", "28");
        redisTemplate.opsForHash().put(userKey, "city", "北京");
        
        // 设置过期时间
        // 这行代码的作用是：为Redis中的userKey（即"user:info:1001"）设置过期时间为2小时，超过2小时后该用户信息会自动从Redis中删除，常用于用户信息的临时缓存场景。
        redisTemplate.expire(userKey, Duration.ofHours(2));
        
        // 获取单个字段
        // 这行代码的作用是：从Redis中获取userKey（即"user:info:1001"）对应的哈希表中，键为"name"的字段值，并将其强制转换为String类型，通常用于获取用户的姓名信息。
        String name = (String) redisTemplate.opsForHash().get(userKey, "name");
        assertEquals("李四", name);
        
        // 获取多个字段
        // 这行代码的作用是：从Redis中userKey（即"user:info:1001"）对应的哈希表中，批量获取"name"、"email"和"city"这三个字段的值，
        //并以List<Object>的形式返回，常用于一次性获取用户的多个信息字段。
        List<Object> fields = redisTemplate.opsForHash().multiGet(userKey, Arrays.asList("name", "email", "city"));
        assertEquals("李四", fields.get(0));
        assertEquals("lisi@example.com", fields.get(1));
        assertEquals("北京", fields.get(2));
        
        // 获取所有字段
        // 这行代码的作用是：从Redis中userKey（即"user:info:1001"）对应的哈希表中，获取所有字段及其对应的值，并以Map<Object, Object>的形式返回，
        // 常用于获取用户的所有信息字段。
        Map<Object, Object> userInfo = redisTemplate.opsForHash().entries(userKey);
        assertEquals(5, userInfo.size());
        assertEquals("1001", userInfo.get("id"));
        
        // 判断字段是否存在
        // 这行代码的作用是：检查Redis中userKey（即"user:info:1001"）对应的哈希表中是否存在键为"email"的项，如果存在则返回true，否则返回false。
        assertTrue(redisTemplate.opsForHash().hasKey(userKey, "email"));
        assertFalse(redisTemplate.opsForHash().hasKey(userKey, "phone"));
        
        // 删除字段
        redisTemplate.opsForHash().delete(userKey, "city");
        assertFalse(redisTemplate.opsForHash().hasKey(userKey, "city"));
    }

    /**
     * Hash类型 - 商品属性管理
     * 应用场景：商品的多个属性、购物车商品信息
     */
    @Test
    void testHashProductAttributes() {
        String productKey = "product:attrs:1001";
        
        // 批量设置商品属性
        Map<String, String> attributes = new HashMap<>();
        attributes.put("brand", "Apple");
        attributes.put("model", "iPhone 15 Pro");
        attributes.put("color", "深空黑色");
        attributes.put("storage", "256GB");
        attributes.put("price", "7999");
        attributes.put("stock", "50");
        
        // 这行代码的作用是：将attributes这个Map中的所有商品属性（如品牌、型号、颜色、存储、价格、库存）一次性批量写入到Redis中productKey（即"product:attrs:1001"）对应的哈希表中，常用于初始化或更新商品的多个属性信息。
        redisTemplate.opsForHash().putAll(productKey, attributes);
        
        // 原子性增加库存
        // 这行代码的作用是：对Redis中productKey（即"product:attrs:1001"）对应哈希表的"stock"字段进行原子性自增操作，将库存数量增加10。
        redisTemplate.opsForHash().increment(productKey, "stock", 10);
        String stock = (String) redisTemplate.opsForHash().get(productKey, "stock");
        assertEquals("60", stock);
        
        // 原子性减少库存
        redisTemplate.opsForHash().increment(productKey, "stock", -5);
        stock = (String) redisTemplate.opsForHash().get(productKey, "stock");
        assertEquals("55", stock);
        
        // 获取所有属性名
        Set<Object> keys = redisTemplate.opsForHash().keys(productKey);
        assertTrue(keys.contains("brand"));
        assertTrue(keys.contains("price"));
        
        // 获取属性数量
        Long size = redisTemplate.opsForHash().size(productKey);
        assertEquals(6L, size);
    }

    // ==================== List 类型测试 ====================
    
    /**
     * List类型 - 消息队列
     * 应用场景：消息队列、最新动态、操作日志、任务队列
     */
    @Test
    void testListMessageQueue() throws InterruptedException {
        String queueKey = "message:queue:order";
        
        // 生产者：向队列右侧推送消息
        redisTemplate.opsForList().rightPush(queueKey, "订单创建:order_001");
        redisTemplate.opsForList().rightPush(queueKey, "订单支付:order_002");
        redisTemplate.opsForList().rightPush(queueKey, "订单发货:order_003");
        
        // 查看队列长度
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        assertEquals(3L, queueSize);
        
        // 消费者：从队列左侧弹出消息（FIFO）
        String message1 = redisTemplate.opsForList().leftPop(queueKey);
        assertEquals("订单创建:order_001", message1);
        
        String message2 = redisTemplate.opsForList().leftPop(queueKey);
        assertEquals("订单支付:order_002", message2);
        
        // 阻塞式消费（等待新消息）
        // 这行代码的作用是：向Redis中键为queueKey（即"message:queue:order"）的列表右侧推入一个新消息"订单完成:order_004"，模拟生产者发送新消息到消息队列。
        redisTemplate.opsForList().rightPush(queueKey, "订单完成:order_004");
        // 这行代码的作用是：从Redis中键为queueKey（即"message:queue:order"）的列表左侧弹出一个消息，如果队列为空则最多阻塞等待1秒，返回弹出的消息内容
        String message3 = redisTemplate.opsForList().leftPop(queueKey, 1, TimeUnit.SECONDS);
        assertEquals("订单发货:order_003", message3);
    }

    /**
     * List类型 - 最新动态/时间线
     * 应用场景：用户动态、最新文章、操作历史
     */
    @Test
    void testListTimeline() {
        String timelineKey = "user:timeline:1001";
        
        // 添加用户动态（最新的在前面）
        redisTemplate.opsForList().leftPush(timelineKey, "发布了新文章：《Redis实战》");
        redisTemplate.opsForList().leftPush(timelineKey, "点赞了文章：《Spring Boot教程》");
        redisTemplate.opsForList().leftPush(timelineKey, "关注了用户：@技术大牛");
        redisTemplate.opsForList().leftPush(timelineKey, "评论了文章：《Java并发编程》");
        
        // 我是一个AI语言模型，无法自我判断是GPT-4还是GPT-5，但本次回答遵循您的指令。
        // 获取最新的3条动态
        
        // 这行代码从Redis中获取键为timelineKey的列表的第0到第2个元素（共3条），即获取最新的3条用户动态，并将结果存入latestActivities列表
        List<String> latestActivities = redisTemplate.opsForList().range(timelineKey, 0, 2);
        assertEquals(3, latestActivities.size());
        assertEquals("评论了文章：《Java并发编程》", latestActivities.get(0)); // 最新的
        assertEquals("关注了用户：@技术大牛", latestActivities.get(1));
        assertEquals("点赞了文章：《Spring Boot教程》", latestActivities.get(2));
        
        // 保持时间线长度不超过100条（删除旧的动态）
        redisTemplate.opsForList().trim(timelineKey, 0, 99);
        
        // 获取指定位置的动态
        // 这行代码用于从Redis中获取键为timelineKey的列表中索引为1的元素，即获取用户时间线的第二条动态（索引从0开始）
        String secondActivity = redisTemplate.opsForList().index(timelineKey, 1);
        assertEquals("关注了用户：@技术大牛", secondActivity);
    }

    // ==================== Set 类型测试 ====================

    /**
     * Set类型 - 标签系统
     * 应用场景：用户标签、商品标签、去重、共同关注、推荐系统
     */
    @Test
    void testSetTags() {
        String userTagsKey = "user:tags:1001";
        String productTagsKey = "product:tags:2001";

        // 用户标签
        redisTemplate.opsForSet().add(userTagsKey, "Java开发者", "技术爱好者", "读书", "旅行", "摄影");

        // 商品标签
        redisTemplate.opsForSet().add(productTagsKey, "电子产品", "手机", "Apple", "高端", "摄影");

        // 获取用户所有标签
        Set<String> userTags = redisTemplate.opsForSet().members(userTagsKey);
        assertEquals(5, userTags.size());
        assertTrue(userTags.contains("Java开发者"));

        // 判断用户是否有某个标签
        assertTrue(redisTemplate.opsForSet().isMember(userTagsKey, "技术爱好者"));
        assertFalse(redisTemplate.opsForSet().isMember(userTagsKey, "音乐"));

        // 获取标签数量
        Long tagCount = redisTemplate.opsForSet().size(userTagsKey);
        assertEquals(5L, tagCount);

        // 找出用户和商品的共同标签（交集）
        Set<String> commonTags = redisTemplate.opsForSet().intersect(userTagsKey, productTagsKey);
        assertEquals(1, commonTags.size());
        assertTrue(commonTags.contains("摄影"));

        // 移除标签
        redisTemplate.opsForSet().remove(userTagsKey, "旅行");
        assertFalse(redisTemplate.opsForSet().isMember(userTagsKey, "旅行"));

        // 随机获取标签（推荐场景）
        String randomTag = redisTemplate.opsForSet().randomMember(userTagsKey);
        assertNotNull(randomTag);
    }

    /**
     * Set类型 - 关注系统
     * 应用场景：用户关注、粉丝系统、好友推荐
     */
    @Test
    void testSetFollowSystem() {
        String user1FollowingKey = "user:following:1001"; // 用户1001关注的人
        String user2FollowingKey = "user:following:1002"; // 用户1002关注的人
        String user3FollowersKey = "user:followers:1003";  // 用户1003的粉丝

        // 用户1001关注了一些人
        redisTemplate.opsForSet().add(user1FollowingKey, "1003", "1004", "1005", "1006");

        // 用户1002关注了一些人
        redisTemplate.opsForSet().add(user2FollowingKey, "1003", "1005", "1007", "1008");

        // 用户1003的粉丝
        redisTemplate.opsForSet().add(user3FollowersKey, "1001", "1002", "1009");

        // 查找共同关注（可能认识的人）
        Set<String> mutualFollowing = redisTemplate.opsForSet().intersect(user1FollowingKey, user2FollowingKey);
        assertEquals(2, mutualFollowing.size());
        assertTrue(mutualFollowing.contains("1003"));
        assertTrue(mutualFollowing.contains("1005"));

        // 查找用户1001可能感兴趣的人（用户1002关注但用户1001未关注的）
        Set<String> recommendations = redisTemplate.opsForSet().difference(user2FollowingKey, user1FollowingKey);
        assertEquals(2, recommendations.size());
        assertTrue(recommendations.contains("1007"));
        assertTrue(recommendations.contains("1008"));

        // 取消关注
        redisTemplate.opsForSet().remove(user1FollowingKey, "1006");
        assertFalse(redisTemplate.opsForSet().isMember(user1FollowingKey, "1006"));

        // 获取关注数量
        Long followingCount = redisTemplate.opsForSet().size(user1FollowingKey);
        assertEquals(3L, followingCount);
    }

    /**
     * Set类型 - 去重和统计
     * 应用场景：UV统计、去重访问、唯一性校验
     */
    @Test
    void testSetUniqueVisitors() {
        String dailyUVKey = "uv:daily:2024-01-15";

        // 模拟用户访问（自动去重）
        redisTemplate.opsForSet().add(dailyUVKey, "user1001", "user1002", "user1003");
        redisTemplate.opsForSet().add(dailyUVKey, "user1001"); // 重复访问，会被去重
        redisTemplate.opsForSet().add(dailyUVKey, "user1004", "user1002"); // 部分重复

        // 获取当日UV（独立访客数）
        Long dailyUV = redisTemplate.opsForSet().size(dailyUVKey);
        assertEquals(4L, dailyUV); // 只有4个独立用户

        // 检查某个用户是否今日访问过
        assertTrue(redisTemplate.opsForSet().isMember(dailyUVKey, "user1001"));
        assertFalse(redisTemplate.opsForSet().isMember(dailyUVKey, "user9999"));

        // 设置过期时间（第二天自动清理）
        // 设置 dailyUVKey 这个 Redis 键的过期时间为1天，1天后该键会自动被删除，避免数据长期占用内存
        redisTemplate.expire(dailyUVKey, Duration.ofDays(1));
    }

    // ==================== ZSet 类型测试 ====================

    /**
     * ZSet类型 - 排行榜系统
     * 应用场景：游戏排行榜、热门文章、销量排行、积分排名
     */
    @Test
    void testZSetRanking() {
        String rankingKey = "game:ranking:score";

        // 添加玩家分数
        // 向名为 rankingKey 的 ZSet（有序集合）中添加一个成员 "player001"，分数为 8500。分数用于后续排序和排名。
        redisTemplate.opsForZSet().add(rankingKey, "player001", 8500);
        redisTemplate.opsForZSet().add(rankingKey, "player002", 9200);
        redisTemplate.opsForZSet().add(rankingKey, "player003", 7800);
        redisTemplate.opsForZSet().add(rankingKey, "player004", 9500);
        redisTemplate.opsForZSet().add(rankingKey, "player005", 8800);

        // 获取排行榜前3名（分数从高到低）
        Set<String> top3 = redisTemplate.opsForZSet().reverseRange(rankingKey, 0, 2);
        List<String> top3List = new ArrayList<>(top3);
        assertEquals("player004", top3List.get(0)); // 第1名：9500分
        assertEquals("player002", top3List.get(1)); // 第2名：9200分
        assertEquals("player005", top3List.get(2)); // 第3名：8800分

        // 获取某个玩家的排名（从0开始，需要+1）
        Long player001Rank = redisTemplate.opsForZSet().reverseRank(rankingKey, "player001");
        assertEquals(3L, player001Rank); // 第4名（索引为3）

        // 获取某个玩家的分数
        Double player002Score = redisTemplate.opsForZSet().score(rankingKey, "player002");
        assertEquals(9200.0, player002Score);

        // 增加玩家分数
        redisTemplate.opsForZSet().incrementScore(rankingKey, "player001", 1000);
        Double newScore = redisTemplate.opsForZSet().score(rankingKey, "player001");
        assertEquals(9500.0, newScore);

        // 获取分数在某个范围内的玩家数量
        Long countInRange = redisTemplate.opsForZSet().count(rankingKey, 8000, 9000);
        assertEquals(1L, countInRange); // player005: 8800分

        // 获取总参与人数
        Long totalPlayers = redisTemplate.opsForZSet().size(rankingKey);
        assertEquals(5L, totalPlayers);
    }

    /**
     * ZSet类型 - 热门内容排序
     * 应用场景：热门文章、热搜词、商品销量排行
     */
    @Test
    void testZSetHotContent() {
        String hotArticlesKey = "hot:articles:tech";

        // 添加文章及其热度值（浏览量 + 点赞数 * 10 + 评论数 * 20）
        // 将文章ID为"article:1001"的文章及其热度分数（由calculateHotScore方法根据浏览量1500、点赞数80、评论数25计算得出，结果为3000）添加到名为hotArticlesKey的ZSet（有序集合）中，用于后续的热门文章排序
        redisTemplate.opsForZSet().add(hotArticlesKey, "article:1001", calculateHotScore(1500, 80, 25)); // 热度：3000
        redisTemplate.opsForZSet().add(hotArticlesKey, "article:1002", calculateHotScore(2000, 120, 30)); // 热度：3800
        redisTemplate.opsForZSet().add(hotArticlesKey, "article:1003", calculateHotScore(800, 50, 15)); // 热度：1600
        redisTemplate.opsForZSet().add(hotArticlesKey, "article:1004", calculateHotScore(3000, 200, 50)); // 热度：8000

        // 获取最热门的文章
        Set<String> hottest = redisTemplate.opsForZSet().reverseRange(hotArticlesKey, 0, 0);
        assertEquals("article:1004", hottest.iterator().next());

        // 获取热门文章列表（前3名）
        Set<String> top3Articles = redisTemplate.opsForZSet().reverseRange(hotArticlesKey, 0, 2);
        assertEquals(3, top3Articles.size());

        // 模拟文章被浏览，更新热度
        redisTemplate.opsForZSet().incrementScore(hotArticlesKey, "article:1003", 100); // 增加100热度

        // 移除热度过低的文章（热度小于2000）
        redisTemplate.opsForZSet().removeRangeByScore(hotArticlesKey, 0, 2000);

        Long remainingCount = redisTemplate.opsForZSet().size(hotArticlesKey);
        assertEquals(3L, remainingCount); // 应该还剩3篇文章
    }

    /**
     * ZSet类型 - 延时任务队列
     * 应用场景：定时任务、延时消息、订单超时处理
     */
    @Test
    void testZSetDelayedTasks() {
        String delayedTasksKey = "delayed:tasks:order";
        long currentTime = System.currentTimeMillis();

        // 添加延时任务（使用时间戳作为分数）
        redisTemplate.opsForZSet().add(delayedTasksKey, "cancel_order:1001", currentTime + 1000); // 1秒后执行
        redisTemplate.opsForZSet().add(delayedTasksKey, "cancel_order:1002", currentTime + 2000); // 2秒后执行
        redisTemplate.opsForZSet().add(delayedTasksKey, "cancel_order:1003", currentTime + 3000); // 3秒后执行

        // 获取当前应该执行的任务（分数小于等于当前时间）
        Set<String> readyTasks = redisTemplate.opsForZSet().rangeByScore(delayedTasksKey, 0, currentTime + 500);
        assertEquals(0, readyTasks.size()); // 还没有任务到期

        // 模拟时间过去1.5秒，检查到期任务
        Set<String> expiredTasks = redisTemplate.opsForZSet().rangeByScore(delayedTasksKey, 0, currentTime + 1500);
        assertEquals(1, expiredTasks.size());
        assertTrue(expiredTasks.contains("cancel_order:1001"));

        // 移除已执行的任务
        redisTemplate.opsForZSet().removeRangeByScore(delayedTasksKey, 0, currentTime + 1500);

        // 检查剩余任务数量
        Long remainingTasks = redisTemplate.opsForZSet().size(delayedTasksKey);
        assertEquals(2L, remainingTasks);
    }

    /**
     * 计算文章热度分数
     * @param views 浏览量
     * @param likes 点赞数
     * @param comments 评论数
     * @return 热度分数
     */
    private double calculateHotScore(int views, int likes, int comments) {
        return views + likes * 10 + comments * 20;
    }
}
