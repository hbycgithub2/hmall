# Redis 使用指南 - 小白友好版

## 📖 什么是Redis？
Redis是一个高性能的内存数据库，就像一个超快的存储柜，可以快速存取数据。

## 🎯 四大常用场景

### 1. 缓存用户信息 💾
**场景说明**：把用户信息存到Redis中，下次查询时直接从Redis获取，不用查数据库，速度更快。

**Redis命令**：
```bash
# 存储用户信息（JSON格式）
SET user:1001 "{'name':'张三','age':25,'email':'zhangsan@example.com'}"

# 获取用户信息
GET user:1001
```

**实际应用**：
- 用户登录后，把用户信息缓存起来
- 用户访问个人中心时，直接从缓存读取
- 减少数据库查询，提升响应速度

### 2. 计数器功能 📊
**场景说明**：统计网站访问量、商品浏览次数等，每次访问就+1。

**Redis命令**：
```bash
# 首页访问量+1
INCR page:views:home

# 商品123访问量+5
INCRBY page:views:product:123 5

# 查看当前访问量
GET page:views:home
```

**实际应用**：
- 统计文章阅读量
- 统计商品浏览次数
- 实时排行榜功能

### 3. 分布式锁 🔒
**场景说明**：在多个服务器环境下，确保同一时间只有一个操作在执行，防止数据冲突。

**Redis命令**：
```bash
# 获取锁（30秒后自动释放，NX表示不存在才设置）
SET lock:order:123 "server1" EX 30 NX

# 释放锁
DEL lock:order:123
```

**实际应用**：
- 防止重复下单
- 防止库存超卖
- 定时任务防重复执行

### 4. 会话存储 🎫
**场景说明**：存储用户登录状态，替代传统的Session机制。

**Redis命令**：
```bash
# 存储会话信息（1小时后过期）
SET session:abc123 "user_id:1001" EX 3600

# 检查会话是否有效
GET session:abc123
```

**实际应用**：
- 用户登录状态管理
- 单点登录(SSO)
- 购物车信息存储

## 🔧 Java代码实现

### 配置Redis连接
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

### 核心服务类
```java
@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 1. 缓存用户信息
    public void cacheUser(Long userId, String userInfo) {
        String key = "user:" + userId;
        redisTemplate.opsForValue().set(key, userInfo);
    }
    
    // 2. 计数器+1
    public Long incrementCounter(String counterKey) {
        return redisTemplate.opsForValue().increment(counterKey);
    }
    
    // 3. 获取分布式锁
    public boolean tryLock(String lockKey, String value, long expireSeconds) {
        return redisTemplate.opsForValue()
            .setIfAbsent(lockKey, value, Duration.ofSeconds(expireSeconds));
    }
    
    // 4. 存储会话
    public void saveSession(String sessionId, String userId, long expireSeconds) {
        String key = "session:" + sessionId;
        redisTemplate.opsForValue().set(key, userId, Duration.ofSeconds(expireSeconds));
    }
}
```

## 📈 性能优势
- **速度快**：内存操作，毫秒级响应
- **并发高**：支持高并发访问
- **功能丰富**：支持多种数据类型
- **持久化**：数据可以持久化到磁盘

## ⚠️ 注意事项
1. **内存限制**：Redis是内存数据库，注意内存使用量
2. **数据过期**：合理设置过期时间，避免内存泄漏
3. **网络延迟**：虽然快，但仍有网络开销
4. **数据一致性**：缓存和数据库的数据一致性问题

## 🚀 快速开始
1. 启动Redis服务
2. 添加Redis依赖到项目
3. 配置Redis连接
4. 使用RedisTemplate操作数据

这样就可以在项目中使用Redis了！
