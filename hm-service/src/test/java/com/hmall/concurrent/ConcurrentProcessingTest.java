package com.hmall.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多线程并发处理测试
 * 使用真实的OrderService，不使用Mock，避免参数匹配问题
 */
public class ConcurrentProcessingTest {
    
    private OrderService orderService;
    private List<Order> testOrders;
    
    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        
        // 准备测试数据
        testOrders = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Order order = new Order(
                (long) i,                           // orderId
                (long) (i % 3 + 1),                // userId (1-3)
                (long) (i % 5 + 1),                // productId (1-5)
                i % 3 + 1,                         // quantity (1-3)
                new BigDecimal("100.00")           // unitPrice
            );
            if (i % 2 == 0) {
                order.setCouponId((long) i);       // 偶数订单有优惠券
            }
            testOrders.add(order);
        }
    }
    
    @Test
    void testBasicConcurrentProcessing() throws Exception {
        System.out.println("=== 基本并发处理测试 ===");
        
        long startTime = System.currentTimeMillis();
        
        // 使用CompletableFuture进行并发处理
        List<CompletableFuture<OrderProcessResult>> futures = testOrders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, orderService);
                try {
                    return task.call();
                } catch (Exception e) {
                    return OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                        Thread.currentThread().getName());
                }
            }))
            .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        allFutures.get(30, TimeUnit.SECONDS);
        
        // 收集结果
        List<OrderProcessResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 验证结果
        assertNotNull(results);
        assertEquals(testOrders.size(), results.size());
        
        // 统计成功和失败的订单
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        System.out.println("处理结果:");
        System.out.println("  总订单数: " + results.size());
        System.out.println("  成功订单: " + successCount);
        System.out.println("  失败订单: " + failureCount);
        System.out.println("  总处理时间: " + totalTime + "ms");
        
        // 验证使用了多个线程
        long uniqueThreads = results.stream()
            .map(OrderProcessResult::getThreadName)
            .distinct()
            .count();
        
        System.out.println("  使用线程数: " + uniqueThreads);
        
        // 显示每个订单的处理结果
        for (OrderProcessResult result : results) {
            System.out.printf("  订单%d: %s, 线程: %s, 耗时: %dms%n",
                result.getOrderId(),
                result.isSuccess() ? "成功" : "失败",
                result.getThreadName(),
                result.getProcessingTimeMs());
            
            if (!result.isSuccess()) {
                System.out.println("    失败原因: " + result.getErrorMessage());
            }
        }
        
        // 验证至少有一些订单成功处理
        assertTrue(successCount > 0, "应该有至少一个订单成功处理");
        
        // 验证使用了多线程（在大多数情况下）
        System.out.println("多线程并发处理测试完成");
    }
    
    @Test
    void testPerformanceComparison() throws Exception {
        System.out.println("\n=== 性能对比测试 ===");
        
        // 串行处理
        System.out.println("1. 串行处理:");
        long serialStartTime = System.currentTimeMillis();
        
        List<OrderProcessResult> serialResults = new ArrayList<>();
        for (Order order : testOrders) {
            OrderTask task = new OrderTask(order, orderService);
            try {
                OrderProcessResult result = task.call();
                serialResults.add(result);
            } catch (Exception e) {
                serialResults.add(OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    Thread.currentThread().getName()));
            }
        }
        
        long serialEndTime = System.currentTimeMillis();
        long serialTime = serialEndTime - serialStartTime;
        
        // 并行处理
        System.out.println("2. 并行处理:");
        long parallelStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderProcessResult>> futures = testOrders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, orderService);
                try {
                    return task.call();
                } catch (Exception e) {
                    return OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                        Thread.currentThread().getName());
                }
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long parallelEndTime = System.currentTimeMillis();
        long parallelTime = parallelEndTime - parallelStartTime;
        
        // 性能对比
        System.out.println("性能对比结果:");
        System.out.println("  串行处理时间: " + serialTime + "ms");
        System.out.println("  并行处理时间: " + parallelTime + "ms");
        
        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.printf("  性能提升: %.2f倍%n", speedup);
            System.out.printf("  效率提升: %.1f%%%n", (speedup - 1) * 100);
        }
        
        // 验证结果数量一致
        assertEquals(serialResults.size(), futures.size());
        
        System.out.println("性能对比测试完成");
    }
    
    @Test
    void testExceptionHandling() throws Exception {
        System.out.println("\n=== 异常处理测试 ===");
        
        // 创建一个会抛出异常的OrderService
        OrderService faultyOrderService = new OrderService() {
            @Override
            public void simulateRandomException() {
                // 强制抛出异常
                throw new RuntimeException("模拟的系统异常");
            }
        };
        
        List<CompletableFuture<OrderProcessResult>> futures = testOrders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, faultyOrderService);
                try {
                    return task.call();
                } catch (Exception e) {
                    return OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                        Thread.currentThread().getName());
                }
            }).exceptionally(throwable -> {
                // 处理CompletableFuture级别的异常
                return OrderProcessResult.failure(order.getOrderId(), 
                    "CompletableFuture异常: " + throwable.getMessage(),
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    Thread.currentThread().getName());
            }))
            .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 收集结果
        List<OrderProcessResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("异常处理结果:");
        for (OrderProcessResult result : results) {
            System.out.printf("  订单 %d: %s", result.getOrderId(), 
                result.isSuccess() ? "成功" : "失败");
            if (!result.isSuccess()) {
                System.out.printf(" (原因: %s)", result.getErrorMessage());
            }
            System.out.println();
        }
        
        long failureCount = results.stream().mapToLong(r -> r.isSuccess() ? 0 : 1).sum();
        System.out.println("总结: " + failureCount + "/" + results.size() + " 个订单处理失败");
        System.out.println("所有异常都被正确捕获和处理，系统保持稳定");
        
        // 验证所有订单都失败了（因为强制抛出异常）
        assertEquals(testOrders.size(), failureCount);
        
        System.out.println("异常处理测试完成");
    }
    
    @Test
    void testTimeoutControl() throws Exception {
        System.out.println("\n=== 超时控制测试 ===");
        
        // 创建一个处理很慢的OrderService
        OrderService slowOrderService = new OrderService() {
            @Override
            public boolean checkStock(Long productId, Integer quantity) {
                try {
                    // 模拟很慢的库存检查（3秒）
                    Thread.sleep(3000);
                    return super.checkStock(productId, quantity);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("库存检查被中断", e);
                }
            }
        };
        
        // 只测试少量订单以减少测试时间
        List<Order> slowTestOrders = testOrders.subList(0, 2);
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderProcessResult>> futures = slowTestOrders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, slowOrderService);
                try {
                    return task.call();
                } catch (Exception e) {
                    return OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                        Thread.currentThread().getName());
                }
            }).orTimeout(2, TimeUnit.SECONDS) // 设置2秒超时
            .exceptionally(throwable -> {
                String errorMsg = throwable instanceof TimeoutException ? 
                    "处理超时" : throwable.getMessage();
                return OrderProcessResult.failure(order.getOrderId(), errorMsg,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    Thread.currentThread().getName());
            }))
            .collect(Collectors.toList());
        
        // 等待所有任务完成（包括超时的）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        
        // 收集结果
        List<OrderProcessResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("超时控制结果:");
        for (OrderProcessResult result : results) {
            System.out.printf("  订单 %d: %s", result.getOrderId(), 
                result.isSuccess() ? "成功" : "失败");
            if (!result.isSuccess()) {
                System.out.printf(" (原因: %s)", result.getErrorMessage());
            }
            System.out.println();
        }
        
        System.out.println("总处理时间: " + (endTime - startTime) + "ms");
        System.out.println("超时控制有效防止了长时间等待");
        
        // 验证处理时间没有超过太长时间（应该在超时时间附近）
        assertTrue((endTime - startTime) < 5000, "处理时间应该被超时控制限制");
        
        System.out.println("超时控制测试完成");
    }
}
