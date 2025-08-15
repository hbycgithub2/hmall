package com.hmall.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 订单批量处理器
 * 使用多线程并发处理多个订单，提高处理效率
 */
@Slf4j
@Component
public class OrderBatchProcessor {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 线程池配置
     */
    private final ThreadPoolExecutor threadPool;
    
    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    public OrderBatchProcessor() {
        // 创建线程池：核心线程数5，最大线程数20，空闲时间60秒
        this.threadPool = new ThreadPoolExecutor(
            5,                          // 核心线程数
            20,                         // 最大线程数
            60L,                        // 空闲时间
            TimeUnit.SECONDS,           // 时间单位
            new LinkedBlockingQueue<>(100), // 工作队列
            new ThreadFactory() {       // 线程工厂
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "OrderProcessor-" + (++counter));
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    /**
     * 批量处理订单 - 使用CompletableFuture
     */
    public List<OrderProcessResult> processOrdersBatch(List<Order> orders) {
        return processOrdersBatch(orders, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * 批量处理订单 - 带超时控制
     */
    public List<OrderProcessResult> processOrdersBatch(List<Order> orders, int timeoutSeconds) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDateTime batchStartTime = LocalDateTime.now();
        log.info("开始批量处理订单，订单数量: {}, 超时时间: {}秒", orders.size(), timeoutSeconds);
        
        // 创建CompletableFuture任务列表
        List<CompletableFuture<OrderProcessResult>> futures = new ArrayList<>();
        
        for (Order order : orders) {
            CompletableFuture<OrderProcessResult> future = CompletableFuture
                .supplyAsync(() -> {
                    OrderTask task = new OrderTask(order, orderService);
                    try {
                        return task.call();
                    } catch (Exception e) {
                        LocalDateTime now = LocalDateTime.now();
                        return OrderProcessResult.failure(order.getOrderId(), e.getMessage(), 
                                                         now, now, Thread.currentThread().getName());
                    }
                }, threadPool)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS) // 设置超时
                .exceptionally(throwable -> {
                    // 处理超时或其他异常
                    LocalDateTime now = LocalDateTime.now();
                    String errorMsg = throwable instanceof TimeoutException ? 
                        "处理超时" : throwable.getMessage();
                    return OrderProcessResult.failure(order.getOrderId(), errorMsg, 
                                                     now, now, Thread.currentThread().getName());
                });
            
            futures.add(future);
        }
        
        // 等待所有任务完成并收集结果
        List<OrderProcessResult> results = new ArrayList<>();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            // 等待所有任务完成
            allFutures.get(timeoutSeconds + 5, TimeUnit.SECONDS);
            
            // 收集结果
            for (CompletableFuture<OrderProcessResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("获取任务结果失败", e);
                }
            }
            
        } catch (TimeoutException e) {
            log.error("批量处理超时", e);
            // 取消未完成的任务
            futures.forEach(future -> future.cancel(true));
        } catch (Exception e) {
            log.error("批量处理异常", e);
        }
        
        LocalDateTime batchEndTime = LocalDateTime.now();
        long totalTime = java.time.Duration.between(batchStartTime, batchEndTime).toMillis();
        
        // 统计处理结果
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        log.info("批量处理完成，总订单数: {}, 成功: {}, 失败: {}, 总耗时: {}ms", 
                orders.size(), successCount, failureCount, totalTime);
        
        return results;
    }
    
    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        return String.format("线程池状态 - 核心线程数: %d, 活跃线程数: %d, 队列大小: %d, 已完成任务数: %d",
            threadPool.getCorePoolSize(),
            threadPool.getActiveCount(),
            threadPool.getQueue().size(),
            threadPool.getCompletedTaskCount());
    }
    
    /**
     * 关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭订单处理线程池...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                log.warn("线程池强制关闭");
            } else {
                log.info("线程池正常关闭");
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
