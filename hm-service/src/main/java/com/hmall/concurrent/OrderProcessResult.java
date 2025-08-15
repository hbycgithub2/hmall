package com.hmall.concurrent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单处理结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcessResult {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 处理是否成功
     */
    private boolean success;
    
    /**
     * 处理消息
     */
    private String message;
    
    /**
     * 最终价格
     */
    private BigDecimal finalPrice;
    
    /**
     * 库存是否充足
     */
    private boolean stockAvailable;
    
    /**
     * 优惠券是否有效
     */
    private boolean couponValid;
    
    /**
     * 处理开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 处理结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 处理耗时（毫秒）
     */
    private long processingTimeMs;
    
    /**
     * 处理线程名称
     */
    private String threadName;
    
    /**
     * 异常信息
     */
    private String errorMessage;
    
    /**
     * 创建成功结果
     */
    public static OrderProcessResult success(Long orderId, BigDecimal finalPrice, 
                                           boolean stockAvailable, boolean couponValid,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           String threadName) {
        OrderProcessResult result = new OrderProcessResult();
        result.setOrderId(orderId);
        result.setSuccess(true);
        result.setMessage("订单处理成功");
        result.setFinalPrice(finalPrice);
        result.setStockAvailable(stockAvailable);
        result.setCouponValid(couponValid);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setProcessingTimeMs(java.time.Duration.between(startTime, endTime).toMillis());
        result.setThreadName(threadName);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static OrderProcessResult failure(Long orderId, String errorMessage,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           String threadName) {
        OrderProcessResult result = new OrderProcessResult();
        result.setOrderId(orderId);
        result.setSuccess(false);
        result.setMessage("订单处理失败");
        result.setErrorMessage(errorMessage);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setProcessingTimeMs(java.time.Duration.between(startTime, endTime).toMillis());
        result.setThreadName(threadName);
        return result;
    }
}
