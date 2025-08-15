package com.hmall.concurrent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simple Order Processing Result
 */
public class SimpleOrderResult {
    
    private Long orderId;
    private boolean success;
    private String message;
    private BigDecimal finalPrice;
    private boolean stockAvailable;
    private boolean couponValid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long processingTimeMs;
    private String threadName;
    private String errorMessage;
    
    public SimpleOrderResult() {}
    
    public SimpleOrderResult(Long orderId, boolean success, String message) {
        this.orderId = orderId;
        this.success = success;
        this.message = message;
    }
    
    /**
     * Create success result
     */
    public static SimpleOrderResult success(Long orderId, BigDecimal finalPrice, 
                                           boolean stockAvailable, boolean couponValid,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           String threadName) {
        SimpleOrderResult result = new SimpleOrderResult();
        result.setOrderId(orderId);
        result.setSuccess(true);
        result.setMessage("Order processed successfully");
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
     * Create failure result
     */
    public static SimpleOrderResult failure(Long orderId, String errorMessage,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           String threadName) {
        SimpleOrderResult result = new SimpleOrderResult();
        result.setOrderId(orderId);
        result.setSuccess(false);
        result.setMessage("Order processing failed");
        result.setErrorMessage(errorMessage);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setProcessingTimeMs(java.time.Duration.between(startTime, endTime).toMillis());
        result.setThreadName(threadName);
        return result;
    }
    
    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    
    public boolean isStockAvailable() { return stockAvailable; }
    public void setStockAvailable(boolean stockAvailable) { this.stockAvailable = stockAvailable; }
    
    public boolean isCouponValid() { return couponValid; }
    public void setCouponValid(boolean couponValid) { this.couponValid = couponValid; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
