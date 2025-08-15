package com.hmall.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Simple Concurrent Processing Demo
 * Demonstrates multi-threaded order processing in e-commerce scenarios
 */
public class SimpleConcurrentDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Multi-threaded Order Processing Demo ===\n");
        
        SimpleConcurrentDemo demo = new SimpleConcurrentDemo();
        
        // Demo 1: Basic concurrent processing
        demo.demonstrateBasicConcurrentProcessing();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 2: Performance comparison
        demo.demonstratePerformanceComparison();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 3: Exception handling
        demo.demonstrateExceptionHandling();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Demo 4: Timeout control
        demo.demonstrateTimeoutControl();
    }
    
    /**
     * Demo 1: Basic concurrent processing
     */
    public void demonstrateBasicConcurrentProcessing() {
        System.out.println("Demo 1: Basic Concurrent Processing");
        System.out.println("-".repeat(30));
        
        OrderService orderService = new OrderService();
        List<Order> orders = createTestOrders(8);
        
        System.out.println("Created " + orders.size() + " test orders");
        System.out.println("Starting concurrent processing...\n");
        
        long startTime = System.currentTimeMillis();
        
        // Use CompletableFuture for concurrent processing
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
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(30, TimeUnit.SECONDS);
            
            // Collect results
            List<OrderProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            
            // Analyze results
            analyzeResults(results, endTime - startTime);
            
        } catch (Exception e) {
            System.err.println("Processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Demo 2: Performance comparison (Serial vs Parallel)
     */
    public void demonstratePerformanceComparison() {
        System.out.println("Demo 2: Performance Comparison (Serial vs Parallel)");
        System.out.println("-".repeat(30));
        
        OrderService orderService = new OrderService();
        List<Order> orders = createTestOrders(10);
        
        System.out.println("Test order count: " + orders.size());
        
        // Serial processing
        System.out.println("\n1. Serial Processing:");
        long serialStartTime = System.currentTimeMillis();
        
        List<OrderProcessResult> serialResults = new ArrayList<>();
        for (Order order : orders) {
            OrderTask task = new OrderTask(order, orderService);
            try {
                OrderProcessResult result = task.call();
                serialResults.add(result);
                System.out.printf("  Order %d completed (thread: %s)\n", 
                    result.getOrderId(), result.getThreadName());
            } catch (Exception e) {
                System.err.println("  Order " + order.getOrderId() + " failed: " + e.getMessage());
            }
        }
        
        long serialEndTime = System.currentTimeMillis();
        long serialTime = serialEndTime - serialStartTime;
        
        // Parallel processing
        System.out.println("\n2. Parallel Processing:");
        long parallelStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<OrderProcessResult>> futures = orders.stream()
            .map(order -> CompletableFuture.supplyAsync(() -> {
                OrderTask task = new OrderTask(order, orderService);
                try {
                    OrderProcessResult result = task.call();
                    System.out.printf("  Order %d completed (thread: %s)\n", 
                        result.getOrderId(), result.getThreadName());
                    return result;
                } catch (Exception e) {
                    System.err.println("  Order " + order.getOrderId() + " failed: " + e.getMessage());
                    return OrderProcessResult.failure(order.getOrderId(), e.getMessage(),
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                        Thread.currentThread().getName());
                }
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
    
    /**
     * Demo 3: Exception handling
     */
    public void demonstrateExceptionHandling() {
        System.out.println("Demo 3: Exception Handling");
        System.out.println("-".repeat(30));
        
        // Create an OrderService that throws exceptions
        OrderService faultyOrderService = new OrderService() {
            @Override
            public void simulateRandomException() {
                // Force exception
                throw new RuntimeException("Simulated system exception");
            }
        };
        
        List<Order> orders = createTestOrders(5);
        
        System.out.println("Processing orders with faulty service...\n");
        
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
                // Handle CompletableFuture level exceptions
                return OrderProcessResult.failure(order.getOrderId(), 
                    "CompletableFuture exception: " + throwable.getMessage(),
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    Thread.currentThread().getName());
            }))
            .collect(Collectors.toList());
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Collect results
        List<OrderProcessResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("Exception handling results:");
        for (OrderProcessResult result : results) {
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
    
    /**
     * Demo 4: Timeout control
     */
    public void demonstrateTimeoutControl() {
        System.out.println("Demo 4: Timeout Control");
        System.out.println("-".repeat(30));
        
        // Create a slow OrderService
        OrderService slowOrderService = new OrderService() {
            @Override
            public boolean checkStock(Long productId, Integer quantity) {
                try {
                    // Simulate very slow stock check (3 seconds)
                    Thread.sleep(3000);
                    return super.checkStock(productId, quantity);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Stock check interrupted", e);
                }
            }
        };
        
        List<Order> orders = createTestOrders(3);
        
        System.out.println("Processing orders with slow service, 2-second timeout...\n");
        
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
            }).orTimeout(2, TimeUnit.SECONDS) // Set 2-second timeout
            .exceptionally(throwable -> {
                String errorMsg = throwable instanceof TimeoutException ? 
                    "Processing timeout" : throwable.getMessage();
                return OrderProcessResult.failure(order.getOrderId(), errorMsg,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                    Thread.currentThread().getName());
            }))
            .collect(Collectors.toList());
        
        // Wait for all tasks to complete (including timed out ones)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        
        // Collect results
        List<OrderProcessResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        System.out.println("Timeout control results:");
        for (OrderProcessResult result : results) {
            System.out.printf("  Order %d: %s", result.getOrderId(), 
                result.isSuccess() ? "Success" : "Failed");
            if (!result.isSuccess()) {
                System.out.printf(" (reason: %s)", result.getErrorMessage());
            }
            System.out.println();
        }
        
        System.out.println("\nTotal processing time: " + (endTime - startTime) + "ms");
        System.out.println("Timeout control effectively prevented long waits");
    }
    
    /**
     * Create test orders
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
            
            // 50% of orders have coupons
            if (i % 2 == 0) {
                order.setCouponId((long) i);
            }
            
            orders.add(order);
        }
        
        return orders;
    }
    
    /**
     * Analyze processing results
     */
    private void analyzeResults(List<OrderProcessResult> results, long totalTime) {
        if (results.isEmpty()) {
            System.out.println("No processing results");
            return;
        }
        
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failureCount = results.size() - successCount;
        
        double avgProcessingTime = results.stream()
            .mapToLong(OrderProcessResult::getProcessingTimeMs)
            .average()
            .orElse(0);
        
        // Count unique threads used
        long uniqueThreads = results.stream()
            .map(OrderProcessResult::getThreadName)
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
        for (OrderProcessResult result : results) {
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
}
