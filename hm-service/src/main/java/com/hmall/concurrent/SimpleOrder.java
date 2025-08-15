package com.hmall.concurrent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simple Order Entity
 */
public class SimpleOrder {
    
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long couponId;
    private String status;
    private LocalDateTime createTime;
    
    public SimpleOrder() {}
    
    public SimpleOrder(Long orderId, Long userId, Long productId, Integer quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = "PENDING";
        this.createTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
