package com.hmall.concurrent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Entity Class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品数量
     */
    private Integer quantity;
    
    /**
     * 商品单价
     */
    private BigDecimal unitPrice;
    
    /**
     * 优惠券ID
     */
    private Long couponId;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 构造方法 - 用于测试
     */
    public Order(Long orderId, Long userId, Long productId, Integer quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = "PENDING";
        this.createTime = LocalDateTime.now();
    }
}
