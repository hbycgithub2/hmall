package com.hmall.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务类 - 实现四大核心功能
 * 1. 用户信息缓存
 * 2. 计数器功能
 * 3. 分布式锁
 * 4. 会话存储
 */
@Service
@Slf4j
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 1. 用户信息缓存 ====================

    /**
     * 缓存用户信息
     * @param userId 用户ID
     * @param userInfo 用户信息对象
     */
    public void cacheUserInfo(Long userId, Object userInfo) {
        try {
            String key = "user:" + userId;
            String jsonValue = objectMapper.writeValueAsString(userInfo);
            redisTemplate.opsForValue().set(key, jsonValue);
            log.info("用户信息已缓存: userId={}", userId);
        } catch (JsonProcessingException e) {
            log.error("缓存用户信息失败: userId={}", userId, e);
        }
    }

    /**
     * 获取缓存的用户信息
     * @param userId 用户ID
     * @param clazz 用户信息类型
     * @return 用户信息对象
     */
    public <T> T getCachedUserInfo(Long userId, Class<T> clazz) {
        try {
            String key = "user:" + userId;
            String jsonValue = (String) redisTemplate.opsForValue().get(key);
            if (jsonValue != null) {
                log.info("从缓存获取用户信息: userId={}", userId);
                return objectMapper.readValue(jsonValue, clazz);
            }
        } catch (JsonProcessingException e) {
            log.error("获取缓存用户信息失败: userId={}", userId, e);
        }
        return null;
    }

    /**
     * 删除用户缓存
     * @param userId 用户ID
     */
    public void deleteUserCache(Long userId) {
        String key = "user:" + userId;
        redisTemplate.delete(key);
        log.info("用户缓存已删除: userId={}", userId);
    }

    // ==================== 2. 计数器功能 ====================

    /**
     * 增加计数器
     * @param counterKey 计数器键
     * @return 增加后的值
     */
    public Long incrementCounter(String counterKey) {
        Long result = redisTemplate.opsForValue().increment(counterKey);
        log.info("计数器增加: key={}, value={}", counterKey, result);
        return result;
    }

    /**
     * 增加计数器指定数量
     * @param counterKey 计数器键
     * @param delta 增加数量
     * @return 增加后的值
     */
    public Long incrementCounterBy(String counterKey, long delta) {
        Long result = redisTemplate.opsForValue().increment(counterKey, delta);
        log.info("计数器增加: key={}, delta={}, value={}", counterKey, delta, result);
        return result;
    }

    /**
     * 获取计数器值
     * @param counterKey 计数器键
     * @return 计数器值
     */
    public Long getCounterValue(String counterKey) {
        Object value = redisTemplate.opsForValue().get(counterKey);
        return value != null ? Long.valueOf(value.toString()) : 0L;
    }

    /**
     * 获取访问量排行榜前N名
     * @param pattern 键模式，如 "page:views:*"
     * @param topN 前N名
     * @return 排行榜
     */
    public Set<ZSetOperations.TypedTuple<Object>> getTopCounters(String pattern, int topN) {
        // 这里简化实现，实际项目中可能需要更复杂的排序逻辑
        return redisTemplate.opsForZSet().reverseRangeWithScores("ranking:" + pattern, 0, topN - 1);
    }

    // ==================== 3. 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁键
     * @param value 锁值（通常是服务器标识）
     * @param expireSeconds 过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String value, long expireSeconds) {
        String key = "lock:" + lockKey;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(expireSeconds));
        
        if (Boolean.TRUE.equals(result)) {
            log.info("获取分布式锁成功: key={}, value={}, expire={}s", key, value, expireSeconds);
            return true;
        } else {
            log.warn("获取分布式锁失败: key={}, value={}", key, value);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁键
     * @param value 锁值（用于验证是否是自己的锁）
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String value) {
        String key = "lock:" + lockKey;
        String currentValue = (String) redisTemplate.opsForValue().get(key);
        
        if (value.equals(currentValue)) {
            redisTemplate.delete(key);
            log.info("释放分布式锁成功: key={}, value={}", key, value);
            return true;
        } else {
            log.warn("释放分布式锁失败，锁值不匹配: key={}, expected={}, actual={}", 
                    key, value, currentValue);
            return false;
        }
    }

    // ==================== 4. 会话存储 ====================

    /**
     * 保存会话信息
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     */
    public void saveSession(String sessionId, String userId, long expireSeconds) {
        String key = "session:" + sessionId;
        redisTemplate.opsForValue().set(key, userId, Duration.ofSeconds(expireSeconds));
        log.info("会话已保存: sessionId={}, userId={}, expire={}s", sessionId, userId, expireSeconds);
    }

    /**
     * 获取会话信息
     * @param sessionId 会话ID
     * @return 用户ID
     */
    public String getSession(String sessionId) {
        String key = "session:" + sessionId;
        String userId = (String) redisTemplate.opsForValue().get(key);
        if (userId != null) {
            log.info("获取会话成功: sessionId={}, userId={}", sessionId, userId);
        } else {
            log.warn("会话不存在或已过期: sessionId={}", sessionId);
        }
        return userId;
    }

    /**
     * 删除会话
     * @param sessionId 会话ID
     */
    public void deleteSession(String sessionId) {
        String key = "session:" + sessionId;
        redisTemplate.delete(key);
        log.info("会话已删除: sessionId={}", sessionId);
    }

    /**
     * 延长会话有效期
     * @param sessionId 会话ID
     * @param expireSeconds 新的过期时间（秒）
     * @return 是否成功
     */
    public boolean extendSession(String sessionId, long expireSeconds) {
        String key = "session:" + sessionId;
        Boolean result = redisTemplate.expire(key, Duration.ofSeconds(expireSeconds));
        if (Boolean.TRUE.equals(result)) {
            log.info("会话有效期已延长: sessionId={}, expire={}s", sessionId, expireSeconds);
            return true;
        } else {
            log.warn("延长会话有效期失败: sessionId={}", sessionId);
            return false;
        }
    }

    // ==================== 通用工具方法 ====================

    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置键的过期时间
     * @param key 键
     * @param seconds 过期时间（秒）
     * @return 是否成功
     */
    public boolean expire(String key, long seconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, Duration.ofSeconds(seconds)));
    }

    /**
     * 获取键的剩余过期时间
     * @param key 键
     * @return 剩余时间（秒），-1表示永不过期，-2表示键不存在
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
