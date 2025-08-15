package com.hmall.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 订单相关业务服务
 * 模拟实际的业务操作，包括库存检查、价格计算、优惠券验证等
 */
@Slf4j
@Service
public class OrderService {
    
    private final Random random = new Random();
    
    /**
     * 检查商品库存
     * 模拟数据库查询操作，有一定的延迟
     */
    public boolean checkStock(Long productId, Integer quantity) {
        try {
            // 模拟数据库查询延迟 100-300ms
            TimeUnit.MILLISECONDS.sleep(100 + random.nextInt(200));
            
            // 模拟库存检查逻辑：90%的概率有库存
            boolean hasStock = random.nextDouble() < 0.9;
            
            log.debug("商品ID: {}, 需要数量: {}, 库存充足: {}, 线程: {}", 
                     productId, quantity, hasStock, Thread.currentThread().getName());
            
            return hasStock;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("库存检查被中断", e);
        }
    }
    
    /**
     * 验证优惠券
     * 模拟优惠券系统调用
     */
    public boolean validateCoupon(Long couponId, Long userId) {
        if (couponId == null) {
            return true; // 没有优惠券也是有效的
        }
        
        try {
            // 模拟优惠券验证延迟 50-150ms
            TimeUnit.MILLISECONDS.sleep(50 + random.nextInt(100));
            
            // 模拟优惠券验证逻辑：85%的概率有效
            boolean isValid = random.nextDouble() < 0.85;
            
            log.debug("优惠券ID: {}, 用户ID: {}, 优惠券有效: {}, 线程: {}", 
                     couponId, userId, isValid, Thread.currentThread().getName());
            
            return isValid;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("优惠券验证被中断", e);
        }
    }
    
    /**
     * 计算订单最终价格
     * 包括商品价格、优惠券折扣等
     */
    public BigDecimal calculateFinalPrice(Order order, boolean couponValid) {
        try {
            // 模拟价格计算延迟 30-80ms
            TimeUnit.MILLISECONDS.sleep(30 + random.nextInt(50));
            
            BigDecimal totalPrice = order.getUnitPrice().multiply(new BigDecimal(order.getQuantity()));
            
            // 如果有有效优惠券，给予5-20%的折扣
            if (couponValid && order.getCouponId() != null) {
                double discountRate = 0.05 + random.nextDouble() * 0.15; // 5%-20%折扣
                BigDecimal discount = totalPrice.multiply(new BigDecimal(discountRate));
                totalPrice = totalPrice.subtract(discount);
                
                log.debug("订单ID: {}, 原价: {}, 折扣率: {:.2f}%, 最终价格: {}, 线程: {}", 
                         order.getOrderId(), order.getUnitPrice().multiply(new BigDecimal(order.getQuantity())), 
                         discountRate * 100, totalPrice, Thread.currentThread().getName());
            } else {
                log.debug("订单ID: {}, 最终价格: {} (无折扣), 线程: {}", 
                         order.getOrderId(), totalPrice, Thread.currentThread().getName());
            }
            
            return totalPrice;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("价格计算被中断", e);
        }
    }
    
    /**
     * 模拟随机异常
     * 用于测试异常处理
     */
    public void simulateRandomException() {
        // 5%的概率抛出异常
        if (random.nextDouble() < 0.05) {
            throw new RuntimeException("模拟的业务异常");
        }
    }
}
