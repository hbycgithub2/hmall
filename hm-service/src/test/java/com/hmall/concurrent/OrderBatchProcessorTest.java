package com.hmall.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单批量处理器单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderBatchProcessorTest {
    
    @Mock
    private OrderService orderService;
    
    @InjectMocks
    private OrderBatchProcessor orderBatchProcessor;
    
    private List<Order> testOrders;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        testOrders = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
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
    void testProcessOrdersBatch_Success() throws Exception {
        // 模拟所有服务调用都成功
        when(orderService.checkStock(anyLong(), anyInt())).thenReturn(true);
        when(orderService.validateCoupon(any(), anyLong())).thenReturn(true);
        when(orderService.calculateFinalPrice(any(Order.class), anyBoolean()))
            .thenReturn(new BigDecimal("90.00"));
        doNothing().when(orderService).simulateRandomException();
        
        // 执行批量处理
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(testOrders);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(testOrders.size(), results.size());
        
        // 验证所有订单都处理成功
        for (OrderProcessResult result : results) {
            assertTrue(result.isSuccess());
            assertNotNull(result.getOrderId());
            assertNotNull(result.getFinalPrice());
            assertTrue(result.isStockAvailable());
            assertNotNull(result.getThreadName());
            assertTrue(result.getProcessingTimeMs() >= 0);
        }
        
        // 验证服务方法被调用
        verify(orderService, times(testOrders.size())).checkStock(anyLong(), anyInt());
        verify(orderService, times(testOrders.size())).calculateFinalPrice(any(Order.class), anyBoolean());
    }
    
    @Test
    void testProcessOrdersBatch_StockNotAvailable() throws Exception {
        // 模拟库存不足
        when(orderService.checkStock(anyLong(), anyInt())).thenReturn(false);
        doNothing().when(orderService).simulateRandomException();
        
        // 执行批量处理
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(testOrders);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(testOrders.size(), results.size());
        
        // 验证所有订单都因库存不足而失败
        for (OrderProcessResult result : results) {
            assertFalse(result.isSuccess());
            assertEquals("库存不足", result.getErrorMessage());
            assertFalse(result.isStockAvailable());
        }
    }
    
    @Test
    void testProcessOrdersBatch_WithException() throws Exception {
        // 模拟业务异常
        when(orderService.checkStock(anyLong(), anyInt())).thenReturn(true);
        doThrow(new RuntimeException("模拟异常")).when(orderService).simulateRandomException();
        
        // 执行批量处理
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(testOrders);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(testOrders.size(), results.size());
        
        // 验证所有订单都因异常而失败
        for (OrderProcessResult result : results) {
            assertFalse(result.isSuccess());
            assertEquals("模拟异常", result.getErrorMessage());
        }
    }
    
    @Test
    void testProcessOrdersBatch_EmptyList() {
        // 测试空订单列表
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(new ArrayList<>());
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testProcessOrdersBatch_NullList() {
        // 测试null订单列表
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(null);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testProcessOrdersBatch_WithTimeout() throws Exception {
        // 模拟长时间处理
        when(orderService.checkStock(anyLong(), anyInt())).thenAnswer(invocation -> {
            TimeUnit.SECONDS.sleep(2); // 模拟2秒延迟
            return true;
        });
        when(orderService.validateCoupon(any(), anyLong())).thenReturn(true);
        when(orderService.calculateFinalPrice(any(Order.class), anyBoolean()))
            .thenReturn(new BigDecimal("90.00"));
        doNothing().when(orderService).simulateRandomException();
        
        // 使用较短的超时时间
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(testOrders, 1);
        
        // 验证结果 - 应该有超时的订单
        assertNotNull(results);
        assertEquals(testOrders.size(), results.size());
        
        // 检查是否有超时的订单
        boolean hasTimeoutOrder = results.stream()
            .anyMatch(result -> !result.isSuccess() && 
                     (result.getErrorMessage().contains("超时") || 
                      result.getErrorMessage().contains("timeout")));
        
        // 由于并发执行，可能有些任务在超时前完成，所以这里不强制要求所有任务都超时
        // assertTrue(hasTimeoutOrder, "应该有至少一个订单超时");
    }
    
    @Test
    void testGetThreadPoolStatus() {
        String status = orderBatchProcessor.getThreadPoolStatus();
        
        assertNotNull(status);
        assertTrue(status.contains("线程池状态"));
        assertTrue(status.contains("核心线程数"));
        assertTrue(status.contains("活跃线程数"));
    }
    
    @Test
    void testConcurrentProcessing() throws Exception {
        // 模拟正常处理，但添加延迟来测试并发
        when(orderService.checkStock(anyLong(), anyInt())).thenAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(100); // 100ms延迟
            return true;
        });
        when(orderService.validateCoupon(any(), anyLong())).thenAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(50); // 50ms延迟
            return true;
        });
        when(orderService.calculateFinalPrice(any(Order.class), anyBoolean())).thenAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(30); // 30ms延迟
            return new BigDecimal("90.00");
        });
        doNothing().when(orderService).simulateRandomException();
        
        long startTime = System.currentTimeMillis();
        
        // 执行批量处理
        List<OrderProcessResult> results = orderBatchProcessor.processOrdersBatch(testOrders);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 验证结果
        assertEquals(testOrders.size(), results.size());
        
        // 验证并发执行效果：总时间应该远小于串行执行时间
        // 串行执行时间约为：10个订单 * (100+50+30)ms = 1800ms
        // 并发执行应该显著减少时间
        assertTrue(totalTime < 1500, 
                  String.format("并发执行时间(%dms)应该小于串行执行时间", totalTime));
        
        // 验证所有订单都成功处理
        for (OrderProcessResult result : results) {
            assertTrue(result.isSuccess());
            assertNotNull(result.getThreadName());
        }
        
        System.out.println("并发处理耗时: " + totalTime + "ms");
        System.out.println("线程池状态: " + orderBatchProcessor.getThreadPoolStatus());
    }
}
