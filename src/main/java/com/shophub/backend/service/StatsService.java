package com.shophub.backend.service;

import com.shophub.backend.dto.StatsDto;
import com.shophub.backend.dto.StatsDto.DayStat;
import com.shophub.backend.dto.StatsDto.TopProduct;
import com.shophub.backend.entity.Order;
import com.shophub.backend.entity.OrderItem;
import com.shophub.backend.entity.Product;
import com.shophub.backend.repository.OrderRepository;
import com.shophub.backend.repository.ProductRepository;
import com.shophub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class StatsService {

    private static final List<String> STATUSES = List.of("pending", "processing", "shipped", "delivered", "cancelled");
    private static final int DAYS = 14;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ZoneId zone;

    public StatsService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        @Value("${shophub.stats.zone:Asia/Kolkata}") String zone) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public StatsDto compute() {
        List<Order> all = orderRepository.findAllByOrderByCreatedAtDesc();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        STATUSES.forEach(s -> byStatus.put(s, 0L));
        all.forEach(o -> byStatus.merge(o.getStatus(), 1L, Long::sum));

        // "sales" = orders that were neither cancelled nor refunded after a return
        List<Order> sales = all.stream()
                .filter(o -> !"cancelled".equals(o.getStatus()) && !"approved".equals(o.getReturnStatus()))
                .toList();

        BigDecimal revenue = sales.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal avg = sales.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        // last 14 days, oldest first, days without sales included as zero
        LocalDate today = LocalDate.now(zone);
        Map<LocalDate, BigDecimal> dayRevenue = new HashMap<>();
        Map<LocalDate, Long> dayOrders = new HashMap<>();
        for (Order o : sales) {
            LocalDate d = o.getCreatedAt().atZone(zone).toLocalDate();
            dayRevenue.merge(d, o.getTotal(), BigDecimal::add);
            dayOrders.merge(d, 1L, Long::sum);
        }
        List<DayStat> days = new ArrayList<>();
        for (int i = DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            days.add(new DayStat(d.toString(),
                    dayRevenue.getOrDefault(d, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                    dayOrders.getOrDefault(d, 0L)));
        }

        // best sellers
        Map<Long, long[]> units = new HashMap<>();
        Map<Long, BigDecimal> money = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Order o : sales) {
            for (OrderItem i : o.getItems()) {
                units.computeIfAbsent(i.getProductId(), k -> new long[1])[0] += i.getQuantity();
                money.merge(i.getProductId(), i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())), BigDecimal::add);
                names.put(i.getProductId(), i.getName());
            }
        }
        List<TopProduct> top = units.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> new TopProduct(e.getKey(), names.get(e.getKey()), e.getValue()[0],
                        money.get(e.getKey()).setScale(2, RoundingMode.HALF_UP)))
                .toList();

        List<Product> products = productRepository.findAll();
        long low = products.stream().filter(p -> p.getStock() != null && p.getStock() <= 5).count();
        long customers = userRepository.findAll().stream().filter(u -> !"admin".equalsIgnoreCase(u.getRole())).count();
        long pendingReturns = all.stream().filter(o -> "requested".equals(o.getReturnStatus())).count();

        return new StatsDto(revenue, sales.size(), avg, customers, products.size(), low, pendingReturns,
                byStatus, days, top);
    }
}
