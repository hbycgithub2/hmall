### 性能对比：串行处理 vs 并行处理（基于 `testPerformanceComparison`）

本文基于 `hm-service/src/test/java/com/hmall/concurrent/SimpleConcurrentTest.java` 中的 `testPerformanceComparison` 用例，梳理串行与并行两种处理方式的流程、关键代码与注意事项，并配套了时序图（见 `testPerformanceComparison.puml`）。

---

### 串行处理流程
- 记录开始时间 `serialStartTime`
- 依次对每个 `Order`：创建 `OrderTask`，同步调用 `call()`，收集 `OrderProcessResult`
- 记录结束时间并计算 `serialTime`

关键代码（串行处理）：

```183:199:hm-service/src/test/java/com/hmall/concurrent/SimpleConcurrentTest.java
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
```

---

### 并行处理流程
- 记录开始时间 `parallelStartTime`
- 使用 `CompletableFuture.supplyAsync` 为每个 `Order` 提交异步任务，内部仍执行 `OrderTask.call()`
- 通过 `CompletableFuture.allOf(...).join()` 等待全部完成
- 记录结束时间并计算 `parallelTime`，再计算 `speedup = serialTime / parallelTime`

关键代码（并行处理）：

```200:219:hm-service/src/test/java/com/hmall/concurrent/SimpleConcurrentTest.java
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
```

输出与加速比：

```221:227:hm-service/src/test/java/com/hmall/concurrent/SimpleConcurrentTest.java
System.out.println("串行处理时间: " + serialTime + "ms");
System.out.println("并行处理时间: " + parallelTime + "ms");

if (parallelTime > 0) {
    double speedup = (double) serialTime / parallelTime;
    System.out.println("性能提升: " + String.format("%.2f", speedup) + "倍");
}
```

---

### 注意事项与实践建议
- 并行不一定总是更快：当任务数量少、任务极短或线程切换开销较大时，并行可能不占优。
- 指定线程池：生产中建议显式传入受控的线程池（避免创建过多的默认线程）。
- 异常、超时与降级：需要在 `CompletableFuture` 上链式编排异常处理与超时控制；必要时做限流/背压。
- 结果聚合与监控：记录成功/失败、平均耗时、使用线程数等指标，辅助评估并发策略收益。

---

### 时序图
时序图文件：`hm-service/docs/testPerformanceComparison.puml`
可用任意 PlantUML 渲染器或 IDE 插件预览。



