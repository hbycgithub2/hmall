# Redis 5种数据类型实际应用场景详解

## 📖 概述

Redis提供了5种基本数据类型，每种类型都有其特定的应用场景和优势。本文档详细介绍了这些数据类型在实际项目中的应用。

## 🔤 1. String 类型

### 特点
- 最简单的数据类型
- 可以存储字符串、数字、二进制数据
- 最大可存储512MB
- 支持原子性的增减操作

### 实际应用场景

#### 1.1 缓存用户Session
```bash
# 存储用户登录信息
SET session:user:1001 "张三" EX 1800  # 30分钟过期

# 获取用户信息
GET session:user:1001
```

#### 1.2 分布式锁
```bash
# 获取锁（30秒自动释放）
SET lock:order:1001 "locked" NX EX 30

# 释放锁
DEL lock:order:1001
```

#### 1.3 计数器功能
```bash
# 页面访问量统计
INCR page:views:home

# 商品库存扣减
DECR item:stock:1001

# 点赞数增加
INCRBY item:likes:1001 5
```

#### 1.4 缓存复杂对象
```bash
# 缓存商品详情（JSON格式）
SET product:1001 '{"id":1001,"name":"iPhone 15","price":7999}' EX 3600
```

### 优势
- 操作简单，性能最高
- 支持原子性操作
- 适合简单的键值存储

## 🗂️ 2. Hash 类型

### 特点
- 类似于Java中的HashMap
- 一个key对应多个field-value对
- 适合存储对象的多个属性
- 可以单独操作某个字段

### 实际应用场景

#### 2.1 用户信息存储
```bash
# 存储用户的多个属性
HSET user:1001 name "李四" email "lisi@example.com" age 28 city "北京"

# 获取单个属性
HGET user:1001 name

# 获取多个属性
HMGET user:1001 name email city

# 获取所有属性
HGETALL user:1001
```

#### 2.2 商品属性管理
```bash
# 设置商品属性
HSET product:1001 brand "Apple" model "iPhone 15" color "黑色" price 7999 stock 50

# 原子性修改库存
HINCRBY product:1001 stock -1

# 检查属性是否存在
HEXISTS product:1001 warranty
```

#### 2.3 购物车管理
```bash
# 添加商品到购物车
HSET cart:user:1001 item:1001 2 item:1002 1 item:1003 3

# 修改商品数量
HINCRBY cart:user:1001 item:1001 1

# 删除商品
HDEL cart:user:1001 item:1002
```

### 优势
- 节省内存空间
- 可以单独操作对象的某个字段
- 比String存储JSON更灵活

## 📋 3. List 类型

### 特点
- 有序的字符串列表
- 支持从两端插入和弹出
- 可以用作栈或队列
- 支持阻塞操作

### 实际应用场景

#### 3.1 消息队列
```bash
# 生产者推送消息
RPUSH message:queue "订单创建:order_001"
RPUSH message:queue "订单支付:order_002"

# 消费者获取消息（FIFO）
LPOP message:queue

# 阻塞式消费
BLPOP message:queue 10  # 等待10秒
```

#### 3.2 最新动态/时间线
```bash
# 添加用户动态（最新的在前面）
LPUSH user:timeline:1001 "发布了新文章"
LPUSH user:timeline:1001 "点赞了文章"

# 获取最新的5条动态
LRANGE user:timeline:1001 0 4

# 保持时间线长度不超过100条
LTRIM user:timeline:1001 0 99
```

#### 3.3 任务队列
```bash
# 添加任务
RPUSH task:queue:email "send_email:user1001"

# 工作进程获取任务
BLPOP task:queue:email 0  # 无限等待
```

#### 3.4 操作历史记录
```bash
# 记录用户操作
LPUSH user:history:1001 "登录系统"
LPUSH user:history:1001 "查看商品详情"

# 获取最近10次操作
LRANGE user:history:1001 0 9
```

### 优势
- 支持双端操作
- 可以实现栈和队列
- 支持阻塞操作，适合消息队列

## 🎯 4. Set 类型

### 特点
- 无序的字符串集合
- 元素唯一，自动去重
- 支持集合运算（交集、并集、差集）
- 适合去重和关系运算

### 实际应用场景

#### 4.1 标签系统
```bash
# 用户标签
SADD user:tags:1001 "Java开发者" "技术爱好者" "读书" "旅行"

# 商品标签
SADD product:tags:1001 "电子产品" "手机" "Apple" "摄影"

# 查找共同标签
SINTER user:tags:1001 product:tags:1001
```

#### 4.2 关注系统
```bash
# 用户A关注的人
SADD user:following:1001 1003 1004 1005

# 用户B关注的人
SADD user:following:1002 1003 1005 1007

# 共同关注
SINTER user:following:1001 user:following:1002

# 可能认识的人（B关注但A未关注的）
SDIFF user:following:1002 user:following:1001
```

#### 4.3 UV统计（独立访客）
```bash
# 记录每日访问用户（自动去重）
SADD uv:daily:2024-01-15 user1001 user1002 user1003

# 获取当日UV数
SCARD uv:daily:2024-01-15

# 检查用户是否今日访问过
SISMEMBER uv:daily:2024-01-15 user1001
```

#### 4.4 权限管理
```bash
# 用户权限
SADD user:permissions:1001 "read" "write" "delete"

# 角色权限
SADD role:admin "read" "write" "delete" "admin"

# 检查权限
SISMEMBER user:permissions:1001 "delete"
```

### 优势
- 自动去重
- 支持集合运算
- 适合关系型数据处理

## 🏆 5. ZSet (Sorted Set) 类型

### 特点
- 有序的字符串集合
- 每个元素关联一个分数(score)
- 按分数排序，分数可重复
- 支持范围查询

### 实际应用场景

#### 5.1 排行榜系统
```bash
# 添加玩家分数
ZADD game:ranking player001 8500 player002 9200 player003 7800

# 获取排行榜前3名
ZREVRANGE game:ranking 0 2

# 获取某玩家排名
ZREVRANK game:ranking player001

# 增加分数
ZINCRBY game:ranking 1000 player001
```

#### 5.2 热门内容排序
```bash
# 添加文章热度
ZADD hot:articles article:1001 3000 article:1002 3800 article:1003 1600

# 获取最热文章
ZREVRANGE hot:articles 0 9

# 移除热度过低的文章
ZREMRANGEBYSCORE hot:articles 0 1000
```

#### 5.3 延时任务队列
```bash
# 添加延时任务（时间戳作为分数）
ZADD delayed:tasks 1640995200 "cancel_order:1001"

# 获取到期任务
ZRANGEBYSCORE delayed:tasks 0 1640995200

# 移除已执行任务
ZREMRANGEBYSCORE delayed:tasks 0 1640995200
```

#### 5.4 地理位置排序
```bash
# 按距离排序（使用距离作为分数）
ZADD nearby:shops 1.2 "shop001" 2.5 "shop002" 0.8 "shop003"

# 获取最近的商店
ZRANGE nearby:shops 0 4
```

### 优势
- 自动排序
- 支持范围查询
- 适合排行榜和时间序列数据

## 🎯 选择建议

### 何时使用String
- 简单的键值存储
- 计数器功能
- 分布式锁
- 缓存简单对象

### 何时使用Hash
- 存储对象的多个属性
- 需要单独操作对象字段
- 购物车、用户信息等

### 何时使用List
- 消息队列
- 时间线功能
- 操作历史
- 需要保持插入顺序

### 何时使用Set
- 去重需求
- 标签系统
- 关注关系
- 权限管理

### 何时使用ZSet
- 排行榜
- 热门内容
- 延时任务
- 需要按分数排序的场景

## 🚀 性能对比

| 数据类型 | 插入性能 | 查询性能 | 内存占用 | 适用场景 |
|---------|---------|---------|---------|---------|
| String  | 最高     | 最高     | 中等     | 简单存储 |
| Hash    | 高       | 高       | 最低     | 对象存储 |
| List    | 高       | 中等     | 中等     | 队列/栈  |
| Set     | 高       | 高       | 中等     | 去重/集合运算 |
| ZSet    | 中等     | 中等     | 最高     | 排序/排行榜 |

## 📝 最佳实践

1. **合理选择数据类型**：根据业务场景选择最适合的数据类型
2. **设置过期时间**：避免内存泄漏，合理设置TTL
3. **键名规范**：使用有意义的键名，便于管理
4. **批量操作**：使用pipeline或批量命令提高性能
5. **监控内存使用**：定期清理无用数据，监控内存占用
