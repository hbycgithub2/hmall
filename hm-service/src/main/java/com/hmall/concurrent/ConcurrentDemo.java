package com.hmall.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多线程并发处理演示程序
 * 展示电商订单批量处理的实际应用场景
 */
public class ConcurrentDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 多线程并发订单处理演示 ===\n");
        
        ConcurrentDemo demo = new ConcurrentDemo();
        
        // 演示1：基本并发处理
        demo.demonstrateBasicConcurrentProcessing();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 演示2：性能对比
        demo.demonstratePerformanceComparison();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 演示3：异常处理
        demo.demonstrateExceptionHandling();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 演示4：超时控制
        demo.demonstrateTimeoutControl();
    }
    
    /**
     * 演示1：基本并发处理
     */
    public void demonstrateBasicConcurrentProcessing() {
        System.out.println("演示1：基本并发处理");
        System.out.println("-".repeat(30));
        
        OrderService orderService = new OrderService();
        List<Order> orders = createTestOrders(8);
        
        System.out.println("创建了 " + orders.size() + " 个测试订单");
        System.out.println("开始并发处理...\n");
        
        long startTime = System.currentTimeMillis();
        
        // 使用CompletableFuture进行并发处理
        List<CompletableFuture<OrderProcessResult>> futures = orders.stream()
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
        
        try {
            allFutures.get(30, TimeUnit.SECONDS);
            
            // 收集结果
            List<OrderProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            
            // 分析结果
            analyzeResults(results, endTime - startTime);
            
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 演示2：性能对比（串行 vs 并行）
     */
    public void demonstratePerformanceComparison() {
        System.out.println("演示2：性能对比（串行 vs 并行）");
        System.out.println("-".repeat(30));
        
        OrderService orderService = new OrderService();
        List<Order> orders = createTestOrders(10);
        
        System.out.println("测试订单数量: " + orders.size());
        
        // 串行处理
        System.out.println("\n1. 串行处理:");
        long serialStartTime = System.currentTimeMillis();
        
        List<OrderProcessResult> serialResults = new ArrayList<>();
        for (Order order : orders) {
            OrderTask task = new OrderTask(order, orderService);
            try {
                OrderProcessResult result = task.call();
                serialResults.add(result);
                System.out.printf("  订单 %d 处理完成 (线程: %s)\n", 
                    result.getOrderId(), result.getThreadName());
            } catch (Exception e) {
                System.err.println("  订单 " + order.getOrderId() + " 处理失败: " + e.getMessage());
            }
        }
        
        long serialEndTime = System.currentTimeMillis();
        long serialTime = serialEndTime - serialStartTime;
        
        // 并行处理
        System.out.println("\n2. 并行处理:");
        long parallelStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderProcessResult>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, orderService);
                try {
                    OrderProcessResult result = task.call();
                    System.out.printf("  订单 %d 处理完成 (线程: %s)\n", 
                        result.getOrderId(), result.getThreadName());
                    return result;
                } catch (Exception e) {
                    System.err.println("  订单 " + order.getOrderId() + " 处理失败: " + e.getMessage());
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
        System.out.println("\n性能对比结果:");
        System.out.println("  串行处理时间: " + serialTime + "ms");
        System.out.println("  并行处理时间: " + parallelTime + "ms");
        
        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.printf("  性能提升: %.2f倍\n", speedup);
            System.out.printf("  效率提升: %.1f%%\n", (speedup - 1) * 100);
        }
    }
    
    /**
     * 演示3：异常处理
     */
    public void demonstrateExceptionHandling() {
        System.out.println("演示3：异常处理");
        System.out.println("-".repeat(30));
        
        // 创建一个会抛出异常的OrderService
        OrderService faultyOrderService = new OrderService() {
            @Override
            public void simulateRandomException() {
                // 强制抛出异常
                throw new RuntimeException("模拟的系统异常");
            }
        };
        
        List<Order> orders = createTestOrders(5);
        
        System.out.println("使用会抛出异常的服务处理订单...\n");
        
        List<CompletableFuture<OrderProcessResult>> futures = orders.stream()
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
        System.out.println("\n总结: " + failureCount + "/" + results.size() + " 个订单处理失败");
        System.out.println("所有异常都被正确捕获和处理，系统保持稳定");
    }
    
    /**
     * 演示4：超时控制
     */
    public void demonstrateTimeoutControl() {
        System.out.println("演示4：超时控制");
        System.out.println("-".repeat(30));
        
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
        
        List<Order> orders = createTestOrders(3);
        
        System.out.println("使用慢速服务处理订单，设置2秒超时...\n");
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderProcessResult>> futures = orders.stream()
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
        
        System.out.println("\n总处理时间: " + (endTime - startTime) + "ms");
        System.out.println("超时控制有效防止了长时间等待");
    }
    
    /**
     * 创建测试订单
     */
    private List<Order> createTestOrders(int count) {
        List<Order> orders = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Order order = new Order(
                (long) i,                                    // orderId
                (long) (i % 5 + 1),                         // userId (1-5)
                (long) (i % 10 + 1),                        // productId (1-10)
                i % 3 + 1,                                  // quantity (1-3)
                new BigDecimal(100 + i * 10)                // unitPrice
            );
            
            // 50%的订单有优惠券
            if (i % 2 == 0) {
                order.setCouponId((long) i);
            }
            
            orders.add(order);
        }
        
        return orders;
    }
    
    /**
     * 分析处理结果
     */
    private void analyzeResults(List<OrderProcessResult> results, long totalTime) {
        if (results.isEmpty()) {
            System.out.println("没有处理结果");
            return;
        }
        
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        double avgProcessingTime = results.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .average()
            .orElse(0);
        
        // 统计使用的线程数
        long uniqueThreads = results.stream()
            .map(OrderProcessResult::getThreadName)
            .distinct()
            .count();
        
        System.out.println("处理结果统计:");
        System.out.println("  总订单数: " + results.size());
        System.out.println("  成功订单: " + successCount + " (" + 
            String.format("%.1f", (double) successCount / results.size() * 100) + "%)");
        System.out.println("  失败订单: " + failureCount + " (" + 
            String.format("%.1f", (double) failureCount / results.size() * 100) + "%)");
        System.out.println("  总处理时间: " + totalTime + "ms");
        System.out.println("  平均单订单处理时间: " + String.format("%.1f", avgProcessingTime) + "ms");
        System.out.println("  使用线程数: " + uniqueThreads);
        
        // 显示每个订单的详细结果
        System.out.println("\n详细结果:");
        for (OrderProcessResult result : results) {
            System.out.printf("  订单%d: %s, 线程: %s, 耗时: %dms", 
                result.getOrderId(),
                result.isSuccess() ? "成功" : "失败",
                result.getThreadName(),
                result.getProcessingTimeMs());
            
            if (result.isSuccess()) {
                System.out.printf(", 价格: ¥%.2f", result.getFinalPrice());
            } else {
                System.out.printf(", 失败原因: %s", result.getErrorMessage());
            }
            System.out.println();
        }
    }
}
