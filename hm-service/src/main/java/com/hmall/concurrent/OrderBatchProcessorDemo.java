package com.hmall.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 订单批量处理演示类
 * 展示多线程并发处理的实际应用场景
 */
@Slf4j
@Component
public class OrderBatchProcessorDemo implements CommandLineRunner {
    
    @Autowired
    private OrderBatchProcessor orderBatchProcessor;
    
    private final Random random = new Random();
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 订单批量处理演示开始 ===");
        
        // 演示1：正常批量处理
        demonstrateNormalBatchProcessing();
        
        Thread.sleep(2000); // 等待2秒
        
        // 演示2：大批量处理
        demonstrateLargeBatchProcessing();
        
        Thread.sleep(2000); // 等待2秒
        
        // 演示3：性能对比
        demonstratePerformanceComparison();
        
        log.info("=== 订单批量处理演示结束 ===");
    }
    
    /**
     * 演示正常的批量处理
     */
    private void demonstrateNormalBatchProcessing() {
        log.info("\n--- 演示1：正常批量处理 ---");
        
        // 创建测试订单
        List<Order> orders = createTestOrders(10);
        
        // 执行批量处理
        long startTime = System.currentTimeMillis();
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(orders);
        long endTime = System.currentTimeMillis();
        
        // 分析结果
        analyzeResults(results, endTime - startTime);
        
        log.info("线程池状态: {}", orderBatchProcessor.getThreadPoolStatus());
    }
    
    /**
     * 演示大批量处理
     */
    private void demonstrateLargeBatchProcessing() {
        log.info("\n--- 演示2：大批量处理 ---");
        
        // 创建大量测试订单
        List<Order> orders = createTestOrders(50);
        
        // 执行批量处理
        long startTime = System.currentTimeMillis();
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(orders, 60); // 60秒超时
        long endTime = System.currentTimeMillis();
        
        // 分析结果
        analyzeResults(results, endTime - startTime);
        
        log.info("线程池状态: {}", orderBatchProcessor.getThreadPoolStatus());
    }
    
    /**
     * 演示性能对比（模拟串行vs并行）
     */
    private void demonstratePerformanceComparison() {
        log.info("\n--- 演示3：性能对比 ---");
        
        List<Order> orders = createTestOrders(20);
        
        // 并行处理
        long parallelStartTime = System.currentTimeMillis();
        List<OrderProcessResult> parallelResults = orderBatchProcessor.processOrdersBatch(orders);
        long parallelEndTime = System.currentTimeMillis();
        long parallelTime = parallelEndTime - parallelStartTime;
        
        log.info("并行处理结果:");
        analyzeResults(parallelResults, parallelTime);
        
        // 计算理论串行时间（基于平均处理时间）
        double avgProcessingTime = parallelResults.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .average()
            .orElse(0);
        
        long estimatedSerialTime = (long) (avgProcessingTime * orders.size());
        
        log.info("性能对比:");
        log.info("  并行处理时间: {}ms", parallelTime);
        log.info("  估算串行时间: {}ms", estimatedSerialTime);
        log.info("  性能提升: {:.2f}倍", (double) estimatedSerialTime / parallelTime);
        log.info("  并发效率: {:.1f}%", ((double) estimatedSerialTime / parallelTime - 1) * 100);
    }
    
    /**
     * 创建测试订单
     */
    private List<Order> createTestOrders(int count) {
        List<Order> orders = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Order order = new Order(
                (long) i,                                    // orderId
                (long) (random.nextInt(100) + 1),           // userId (1-100)
                (long) (random.nextInt(50) + 1),            // productId (1-50)
                random.nextInt(5) + 1,                      // quantity (1-5)
                new BigDecimal(50 + random.nextInt(200))    // unitPrice (50-249)
            );
            
            // 60%的订单有优惠券
            if (random.nextDouble() < 0.6) {
                order.setCouponId((long) (random.nextInt(20) + 1));
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
            log.info("没有处理结果");
            return;
        }
        
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        double avgProcessingTime = results.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .average()
            .orElse(0);
        
        long maxProcessingTime = results.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .max()
            .orElse(0);
        
        long minProcessingTime = results.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .min()
            .orElse(0);
        
        // 统计使用的线程数
        long uniqueThreads = results.stream()
            .map(OrderProcessResult::getThreadName)
            .distinct()
            .count();
        
        // 计算成功订单的平均价格
        double avgPrice = results.stream()
            .filter(OrderProcessResult::isSuccess)
            .mapToDouble(r -> r.getFinalPrice().doubleValue())
            .average()
            .orElse(0);
        
        log.info("处理结果统计:");
        log.info("  总订单数: {}", results.size());
        log.info("  成功订单: {} ({:.1f}%)", successCount, (double) successCount / results.size() * 100);
        log.info("  失败订单: {} ({:.1f}%)", failureCount, (double) failureCount / results.size() * 100);
        log.info("  总处理时间: {}ms", totalTime);
        log.info("  平均单订单处理时间: {:.1f}ms", avgProcessingTime);
        log.info("  最长处理时间: {}ms", maxProcessingTime);
        log.info("  最短处理时间: {}ms", minProcessingTime);
        log.info("  使用线程数: {}", uniqueThreads);
        log.info("  成功订单平均价格: ¥{:.2f}", avgPrice);
        
        // 显示失败原因统计
        if (failureCount > 0) {
            log.info("失败原因统计:");
            results.stream()
                .filter(r -> !r.isSuccess())
                .collect(java.util.stream.Collectors.groupingBy(
                    OrderProcessResult::getErrorMessage,
                    java.util.stream.Collectors.counting()))
                .forEach((reason, count) -> 
                    log.info("    {}: {} 次", reason, count));
        }
    }
}
