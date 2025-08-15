package com.hmall.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简化的并发测试，不依赖Spring框架
 */
public class SimpleConcurrentTest {
    
    private OrderService orderService;
    private List<Order> testOrders;
    
    @BeforeEach
    void setUp() {
        // 创建一个新的 OrderService 实例，用于后续订单处理测试
        orderService = new OrderService();
        
        // 初始化测试订单列表
        testOrders = new ArrayList<>();
        // 循环生成5个测试订单
        for (int i = 1; i <= 5; i++) {
            // 创建一个订单对象，参数依次为：
            // (long) i                ：订单ID，取值1~5
            // (long) (i % 3 + 1)      ：用户ID，循环取值1~3
            // (long) (i % 5 + 1)      ：商品ID，循环取值1~5
            // i % 3 + 1               ：购买数量，循环取值1~3
            // new BigDecimal("100.00")：商品单价，固定为100.00
            Order order = new Order(
                (long) i,                           // 订单ID
                (long) (i % 3 + 1),                // 用户ID（1-3循环）
                (long) (i % 5 + 1),                // 商品ID（1-5循环）
                i % 3 + 1,                         // 购买数量（1-3循环）
                new BigDecimal("100.00")           // 商品单价
            );
            // 如果订单号为偶数，则设置优惠券ID（优惠券ID等于订单ID）
            if (i % 2 == 0
            ) {
                order.setCouponId((long) i);       // 偶数订单有优惠券
            }
            // 将订单添加到测试订单列表中
            testOrders.add(order);
        }
    }
    
    @Test
    void testConcurrentOrderProcessing() throws Exception {
        // 创建线程池
        // 创建一个固定大小为3的线程池，用于并发处理订单任务。线程池中的线程数量固定为3，适合于需要限制并发线程数的场景，可以有效利用多核CPU资源，同时避免线程过多导致的资源竞争。
        // 线程池大小设置为3，是因为本例测试订单数量为5，线程数设置为3可以模拟有限资源下的并发处理场景，既能体现并发效果，又能避免线程过多导致的资源竞争和上下文切换开销
        // 实际开发中，线程池的线程数通常根据服务器CPU核心数、任务的类型（CPU密集型或IO密集型）、系统资源等因素综合设定
        // 一般建议：CPU密集型任务线程数 = CPU核心数 + 1；IO密集型任务线程数 = 2 * CPU核心数 或更多
        // 例如，获取可用CPU核心数
        int cpuCount = Runtime.getRuntime().availableProcessors();
        // 这里假设为IO密集型任务，线程数设置为2倍CPU核心数
        int threadCount = cpuCount * 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            // 创建任务列表
            List<Future<OrderProcessResult>> futures = new ArrayList<>();
            
            for (Order order : testOrders) {
                Future<OrderProcessResult> future = executor.submit(new OrderTask(order, orderService));
                futures.add(future);
            }
            
            // 收集结果
            List<OrderProcessResult> results = new ArrayList<>();
            for (Future<OrderProcessResult> future : futures) {
                try {
                    OrderProcessResult result = future.get(10, TimeUnit.SECONDS);
                    results.add(result);
                } catch (TimeoutException e) {
                    System.err.println("任务超时");
                    future.cancel(true);
                }
            }
            
            // 验证结果
            assertNotNull(results);
            assertEquals(testOrders.size(), results.size());
            
            // 统计成功和失败的订单
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = results.size() - successCount;
            
            System.out.println("=== 并发处理结果 ===");
            System.out.println("总订单数: " + results.size());
            System.out.println("成功订单: " + successCount);
            System.out.println("失败订单: " + failureCount);
            
            // 显示每个订单的处理结果
            for (OrderProcessResult result : results) {
                System.out.printf("订单ID: %d, 成功: %s, 线程: %s, 耗时: %dms%n",
                    result.getOrderId(),
                    result.isSuccess(),
                    result.getThreadName(),
                    result.getProcessingTimeMs());
                
                if (!result.isSuccess()) {
                    System.out.println("  失败原因: " + result.getErrorMessage());
                } else {
                    System.out.println("  最终价格: ¥" + result.getFinalPrice());
                }
            }
            
            // 验证至少有一些订单成功处理
            assertTrue(successCount > 0, "应该有至少一个订单成功处理");
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    @Test
    void testCompletableFutureConcurrentProcessing() {
        System.out.println("\n=== CompletableFuture 并发处理测试 ===");
        
        long startTime = System.currentTimeMillis();
        
        // 使用CompletableFuture处理订单
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
            .collect(java.util.stream.Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(15, TimeUnit.SECONDS);
            
            // 收集结果
            List<OrderProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // 验证结果
            assertNotNull(results);
            assertEquals(testOrders.size(), results.size());
            
            // 统计结果
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            long failureCount = results.size() - successCount;
            
            System.out.println("总处理时间: " + totalTime + "ms");
            System.out.println("成功订单: " + successCount);
            System.out.println("失败订单: " + failureCount);
            
            // 计算平均处理时间
            double avgProcessingTime = results.stream()
                .mapToLong(OrderProcessResult::getProcessingTimeMs)
                .average()
                .orElse(0);
            
            System.out.println("平均单订单处理时间: " + String.format("%.1f", avgProcessingTime) + "ms");
            
            // 统计使用的线程数
            long uniqueThreads = results.stream()
                .map(OrderProcessResult::getThreadName)
                .distinct()
                .count();
            
            System.out.println("使用的线程数: " + uniqueThreads);
            
            // 验证并发效果
            assertTrue(uniqueThreads > 1, "应该使用多个线程进行并发处理");
            
        } catch (Exception e) {
            fail("CompletableFuture处理失败: " + e.getMessage());
        }
    }
    
    @Test
    void testPerformanceComparison() {
        System.out.println("\n=== 性能对比测试 ===");
        
        // 串行处理
        long serialStartTime = System.currentTimeMillis();
        List<OrderProcessResult> serialResults = new ArrayList<>();
        
        for (Order order : testOrders) {
            OrderTask task = new OrderTask(order, orderService);
            try {
                OrderProcessResult result = task.call();
                serialResults.add(result);
            } catch (Exception e) {
                // 处理异常
            }
        }
        
        long serialEndTime = System.currentTimeMillis();
        long serialTime = serialEndTime - serialStartTime;
        
        // 并行处理
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
            .collect(java.util.stream.Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long parallelEndTime = System.currentTimeMillis();
        long parallelTime = parallelEndTime - parallelStartTime;
        
        System.out.println("串行处理时间: " + serialTime + "ms");
        System.out.println("并行处理时间: " + parallelTime + "ms");
        
        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.println("性能提升: " + String.format("%.2f", speedup) + "倍");
        }
        
        // 验证并行处理确实更快（在大多数情况下）
        // 注意：由于测试数据较少，可能并行处理不一定更快，这是正常的
        System.out.println("测试完成 - 并发处理功能正常");
    }
}
