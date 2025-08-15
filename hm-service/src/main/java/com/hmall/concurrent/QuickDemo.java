package com.hmall.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Quick Multi-threading Demo
 * Simple demonstration without Spring dependencies
 */
public class QuickDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Multi-threaded Order Processing Demo ===\n");
        
        QuickDemo demo = new QuickDemo();
        demo.runDemo();
    }
    
    public void runDemo() {
        // Create test orders
        List<SimpleOrder> orders = createTestOrders(8);
        SimpleOrderService orderService = new SimpleOrderService();
        
        System.out.println("Created " + orders.size() + " test orders");
        System.out.println("Starting concurrent processing...\n");
        
        // Demo 1: Basic concurrent processing
        demonstrateConcurrentProcessing(orders, orderService);
        
        System.out.println("\n" + "==================================================\n");
        
        // Demo 2: Performance comparison
        demonstratePerformanceComparison(orders, orderService);
        
        System.out.println("\n" + "==================================================\n");
        
        // Demo 3: Exception handling
        demonstrateExceptionHandling(orders);
    }
    
    private void demonstrateConcurrentProcessing(List<SimpleOrder> orders, SimpleOrderService orderService) {
        System.out.println("Demo 1: Concurrent Processing");
        System.out.println("------------------------------");
        
        long startTime = System.currentTimeMillis();
        
        // Use CompletableFuture for concurrent processing
        List<CompletableFuture<OrderResult>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> processOrder(order, orderService)))
            .collect(Collectors.toList());
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(30, TimeUnit.SECONDS);
            
            // Collect results
            List<OrderResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            
            analyzeResults(results, endTime - startTime);
            
        } catch (Exception e) {
            System.err.println("Processing failed: " + e.getMessage());
        }
    }
    
    private void demonstratePerformanceComparison(List<SimpleOrder> orders, SimpleOrderService orderService) {
        System.out.println("Demo 2: Performance Comparison (Serial vs Parallel)");
        System.out.println("------------------------------");
        
        // Serial processing
        System.out.println("1. Serial Processing:");
        long serialStartTime = System.currentTimeMillis();
        
        List<OrderResult> serialResults = new ArrayList<>();
        for (SimpleOrder order : orders) {
            OrderResult result = processOrder(order, orderService);
            serialResults.add(result);
            System.out.printf("  Order %d completed (thread: %s)\n", 
                result.getOrderId(), result.getThreadName());
        }
        
        long serialEndTime = System.currentTimeMillis();
        long serialTime = serialEndTime - serialStartTime;
        
        // Parallel processing
        System.out.println("\n2. Parallel Processing:");
        long parallelStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderResult>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderResult result = processOrder(order, orderService);
                System.out.printf("  Order %d completed (thread: %s)\n", 
                    result.getOrderId(), result.getThreadName());
                return result;
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long parallelEndTime = System.currentTimeMillis();
        long parallelTime = parallelEndTime - parallelStartTime;
        
        // Performance comparison
        System.out.println("\nPerformance Comparison:");
        System.out.println("  Serial processing time: " + serialTime + "ms");
        System.out.println("  Parallel processing time: " + parallelTime + "ms");
        
        if (parallelTime > 0) {
            double speedup = (double) serialTime / parallelTime;
            System.out.printf("  Performance improvement: %.2fx\n", speedup);
            System.out.printf("  Efficiency gain: %.1f%%\n", (speedup - 1) * 100);
        }
    }
    
    private void demonstrateExceptionHandling(List<SimpleOrder> orders) {
        System.out.println("Demo 3: Exception Handling");
        System.out.println("------------------------------");
        
        // Create a faulty service that throws exceptions
        SimpleOrderService faultyService = new SimpleOrderService() {
            @Override
            public void simulateRandomException() {
                throw new RuntimeException("Simulated system exception");
            }
        };
        
        System.out.println("Processing orders with faulty service...\n");
        
        List<CompletableFuture<OrderResult>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                try {
                    return processOrder(order, faultyService);
                } catch (Exception e) {
                    return new OrderResult(order.getOrderId(), false, e.getMessage(),
                        null, Thread.currentThread().getName(), 0);
                }
            }).exceptionally(throwable -> {
                return new OrderResult(order.getOrderId(), false, 
                    "CompletableFuture exception: " + throwable.getMessage(),
                    null, Thread.currentThread().getName(), 0);
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        List<OrderResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("Exception handling results:");
        for (OrderResult result : results) {
            System.out.printf("  Order %d: %s", result.getOrderId(), 
                result.isSuccess() ? "Success" : "Failed");
            if (!result.isSuccess()) {
                System.out.printf(" (reason: %s)", result.getErrorMessage());
            }
            System.out.println();
        }
        
        long failureCount = results.stream().mapToLong(r -> r.isSuccess() ? 0 : 1).sum();
        System.out.println("\nSummary: " + failureCount + "/" + results.size() + " orders failed");
        System.out.println("All exceptions were properly caught and handled, system remains stable");
    }
    
    private OrderResult processOrder(SimpleOrder order, SimpleOrderService orderService) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        try {
            // Simulate random exception
            orderService.simulateRandomException();
            
            // 1. Check stock
            boolean stockAvailable = orderService.checkStock(order.getProductId(), order.getQuantity());
            if (!stockAvailable) {
                return new OrderResult(order.getOrderId(), false, "Stock not available",
                    null, threadName, System.currentTimeMillis() - startTime);
            }
            
            // 2. Validate coupon
            boolean couponValid = orderService.validateCoupon(order.getCouponId(), order.getUserId());
            
            // 3. Calculate final price
            BigDecimal finalPrice = orderService.calculateFinalPrice(order, couponValid);
            
            return new OrderResult(order.getOrderId(), true, "Success",
                finalPrice, threadName, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return new OrderResult(order.getOrderId(), false, e.getMessage(),
                null, threadName, System.currentTimeMillis() - startTime);
        }
    }
    
    private List<SimpleOrder> createTestOrders(int count) {
        List<SimpleOrder> orders = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            SimpleOrder order = new SimpleOrder(
                (long) i,                                    // orderId
                (long) (i % 5 + 1),                         // userId (1-5)
                (long) (i % 10 + 1),                        // productId (1-10)
                i % 3 + 1,                                  // quantity (1-3)
                new BigDecimal(100 + i * 10)                // unitPrice
            );
            
            // 50% of orders have coupons
            if (i % 2 == 0) {
                order.setCouponId((long) i);
            }
            
            orders.add(order);
        }
        
        return orders;
    }
    
    private void analyzeResults(List<OrderResult> results, long totalTime) {
        if (results.isEmpty()) {
            System.out.println("No processing results");
            return;
        }
        
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        double avgProcessingTime = results.stream()
            .mapToLong(OrderResult::getProcessingTimeMs)
            .average()
            .orElse(0);
        
        // Count unique threads used
        long uniqueThreads = results.stream()
            .map(OrderResult::getThreadName)
            .distinct()
            .count();
        
        System.out.println("Processing Results Summary:");
        System.out.println("  Total orders: " + results.size());
        System.out.println("  Successful orders: " + successCount + " (" + 
            String.format("%.1f", (double) successCount / results.size() * 100) + "%)");
        System.out.println("  Failed orders: " + failureCount + " (" + 
            String.format("%.1f", (double) failureCount / results.size() * 100) + "%)");
        System.out.println("  Total processing time: " + totalTime + "ms");
        System.out.println("  Average order processing time: " + String.format("%.1f", avgProcessingTime) + "ms");
        System.out.println("  Threads used: " + uniqueThreads);
        
        // Show detailed results for each order
        System.out.println("\nDetailed Results:");
        for (OrderResult result : results) {
            System.out.printf("  Order%d: %s, Thread: %s, Time: %dms", 
                result.getOrderId(),
                result.isSuccess() ? "Success" : "Failed",
                result.getThreadName(),
                result.getProcessingTimeMs());
            
            if (result.isSuccess()) {
                System.out.printf(", Price: $%.2f", result.getFinalPrice());
            } else {
                System.out.printf(", Error: %s", result.getErrorMessage());
            }
            System.out.println();
        }
    }
    
    // Simple result class
    static class OrderResult {
        private final Long orderId;
        private final boolean success;
        private final String errorMessage;
        private final BigDecimal finalPrice;
        private final String threadName;
        private final long processingTimeMs;
        
        public OrderResult(Long orderId, boolean success, String errorMessage, 
                          BigDecimal finalPrice, String threadName, long processingTimeMs) {
            this.orderId = orderId;
            this.success = success;
            this.errorMessage = errorMessage;
            this.finalPrice = finalPrice;
            this.threadName = threadName;
            this.processingTimeMs = processingTimeMs;
        }
        
        // Getters
        public Long getOrderId() { return orderId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public BigDecimal getFinalPrice() { return finalPrice; }
        public String getThreadName() { return threadName; }
        public long getProcessingTimeMs() { return processingTimeMs; }
    }
}
