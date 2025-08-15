package com.hmall.concurrent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * 单个订单处理任务
 * 实现Callable接口，支持返回结果和异常处理
 */
@Slf4j
@AllArgsConstructor
public class OrderTask implements Callable<OrderProcessResult> {
    
    private final Order order;
    private final OrderService orderService;
    
    @Override
    public OrderProcessResult call() {
        LocalDateTime startTime = LocalDateTime.now();
        String threadName = Thread.currentThread().getName();
        
        log.info("开始处理订单: {}, 线程: {}", order.getOrderId(), threadName);
        
        try {
            // 模拟可能的随机异常
            orderService.simulateRandomException();
            
            // 1. 检查库存
            boolean stockAvailable = orderService.checkStock(order.getProductId(), order.getQuantity());
            if (!stockAvailable) {
                LocalDateTime endTime = LocalDateTime.now();
                return OrderProcessResult.failure(order.getOrderId(), "库存不足", 
                                                startTime, endTime, threadName);
            }
            
            // 2. 验证优惠券
            boolean couponValid = orderService.validateCoupon(order.getCouponId(), order.getUserId());
            
            // 3. 计算最终价格
            BigDecimal finalPrice = orderService.calculateFinalPrice(order, couponValid);
            
            LocalDateTime endTime = LocalDateTime.now();
            
            log.info("订单处理完成: {}, 最终价格: {}, 耗时: {}ms, 线程: {}", 
                    order.getOrderId(), finalPrice, 
                    java.time.Duration.between(startTime, endTime).toMillis(), threadName);
            
            return OrderProcessResult.success(order.getOrderId(), finalPrice, 
                                            stockAvailable, couponValid, 
                                            startTime, endTime, threadName);
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            log.error("订单处理失败: {}, 错误: {}, 线程: {}", 
                     order.getOrderId(), e.getMessage(), threadName, e);
            
            return OrderProcessResult.failure(order.getOrderId(), e.getMessage(), 
                                            startTime, endTime, threadName);
        }
    }
}
