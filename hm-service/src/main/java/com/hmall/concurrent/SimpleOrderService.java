package com.hmall.concurrent;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Simple Order Service
 * Simulates business operations like stock check, price calculation, coupon validation
 */
public class SimpleOrderService {

    private final Random random = new Random();

    /**
     * Check product stock
     * Simulates database query with some delay
     */
    public boolean checkStock(Long productId, Integer quantity) {
        try {
            // Simulate database query delay 100-300ms
            TimeUnit.MILLISECONDS.sleep(100 + random.nextInt(200));

            // Simulate stock check logic: 90% probability of having stock
            boolean hasStock = random.nextDouble() < 0.9;

            System.out.printf("Product ID: %d, Quantity needed: %d, Stock available: %s, Thread: %s%n",
                     productId, quantity, hasStock, Thread.currentThread().getName());

            return hasStock;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Stock check interrupted", e);
        }
    }

    /**
     * Validate coupon
     * Simulates coupon system call
     */
    public boolean validateCoupon(Long couponId, Long userId) {
        if (couponId == null) {
            return true; // No coupon is also valid
        }

        try {
            // Simulate coupon validation delay 50-150ms
            TimeUnit.MILLISECONDS.sleep(50 + random.nextInt(100));

            // Simulate coupon validation logic: 85% probability of being valid
            boolean isValid = random.nextDouble() < 0.85;

            System.out.printf("Coupon ID: %d, User ID: %d, Coupon valid: %s, Thread: %s%n",
                     couponId, userId, isValid, Thread.currentThread().getName());

            return isValid;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Coupon validation interrupted", e);
        }
    }

    /**
     * Calculate final order price
     * Includes product price, coupon discount, etc.
     */
    public BigDecimal calculateFinalPrice(SimpleOrder order, boolean couponValid) {
        try {
            // Simulate price calculation delay 30-80ms
            TimeUnit.MILLISECONDS.sleep(30 + random.nextInt(50));

            BigDecimal totalPrice = order.getUnitPrice().multiply(new BigDecimal(order.getQuantity()));

            // If there's a valid coupon, apply 5-20% discount
            if (couponValid && order.getCouponId() != null) {
                double discountRate = 0.05 + random.nextDouble() * 0.15; // 5%-20% discount
                BigDecimal discount = totalPrice.multiply(new BigDecimal(discountRate));
                totalPrice = totalPrice.subtract(discount);

                System.out.printf("Order ID: %d, Original price: %s, Discount rate: %.2f%%, Final price: %s, Thread: %s%n",
                         order.getOrderId(), order.getUnitPrice().multiply(new BigDecimal(order.getQuantity())),
                         discountRate * 100, totalPrice, Thread.currentThread().getName());
            } else {
                System.out.printf("Order ID: %d, Final price: %s (no discount), Thread: %s%n",
                         order.getOrderId(), totalPrice, Thread.currentThread().getName());
            }

            return totalPrice;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Price calculation interrupted", e);
        }
    }

    /**
     * Simulate random exception
     * Used for testing exception handling
     */
    public void simulateRandomException() {
        // 5% probability of throwing exception
        if (random.nextDouble() < 0.05) {
            throw new RuntimeException("Simulated business exception");
        }
    }
}
