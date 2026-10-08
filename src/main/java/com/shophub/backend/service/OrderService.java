package com.shophub.backend.service;

import com.shophub.backend.dto.*;
import com.shophub.backend.entity.Coupon;
import com.shophub.backend.entity.Order;
import com.shophub.backend.entity.OrderEvent;
import com.shophub.backend.entity.OrderItem;
import com.shophub.backend.entity.Product;
import com.shophub.backend.entity.User;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.OrderRepository;
import com.shophub.backend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    /** Normal life of an order. "cancelled" can happen before delivery. */
    private static final List<String> FLOW = List.of("pending", "processing", "shipped", "delivered");
    private static final Set<String> VALID_STATUSES = Set.of("pending", "processing", "shipped", "delivered", "cancelled");
    private static final Set<String> CUSTOMER_CAN_CANCEL = Set.of("pending", "processing");
    private static final int RETURN_WINDOW_DAYS = 7;
    private static final Set<String> PAYMENT_METHODS = Set.of("card", "upi", "cod", "netbanking");

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("999");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("99");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CartService cartService,
                        CouponService couponService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.couponService = couponService;
    }

    @Transactional
    public OrderDto createOrder(User user, OrderRequest req) {
        // combine duplicate lines; sorted by product id so concurrent orders lock rows in the same order
        Map<Long, Integer> quantities = new TreeMap<>();
        for (CartItemRequest line : req.items()) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        // 1) cheap checks first, so a bad request never touches stock
        String payment = req.paymentMethod() == null || req.paymentMethod().isBlank()
                ? "card" : req.paymentMethod().trim().toLowerCase();
        if (!PAYMENT_METHODS.contains(payment)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown payment method");
        }

        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(payment);
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Product, Integer> toReserve = new LinkedHashMap<>();

        // 2) lock every product, check stock and price the order (nothing is changed yet)
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            int qty = entry.getValue();
            if (qty > 100) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum quantity per product is 100");
            }
            // price and stock always come from the database (row is locked until this order is saved)
            Product product = productRepository.lockById(entry.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "A product in your cart is no longer available"));

            Integer stock = product.getStock();
            if (stock != null) {
                if (stock <= 0) {
                    throw new ApiException(HttpStatus.CONFLICT, product.getName() + " is out of stock");
                }
                if (stock < qty) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Only " + stock + " left in stock for " + product.getName());
                }
            }
            toReserve.put(product, qty);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setPrice(product.getPrice());
            item.setImage(product.getImage());
            item.setCategory(product.getCategory());
            item.setQuantity(qty);
            order.getItems().add(item);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // 3) coupon -> discount on the subtotal; shipping and tax are worked out on the discounted amount
        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            coupon = couponService.requireUsable(req.couponCode(), subtotal);
            discount = couponService.discountFor(coupon, subtotal);
            order.setCouponCode(coupon.getCode());
        }
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal shipping = taxable.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_FEE;
        BigDecimal tax = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(shipping).add(tax);

        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDiscount(discount.setScale(2, RoundingMode.HALF_UP));
        order.setShipping(shipping.setScale(2, RoundingMode.HALF_UP));
        order.setTax(tax);
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        AddressDto a = req.shippingAddress();
        order.setShippingFirstName(orEmpty(a.firstName()));
        order.setShippingLastName(orEmpty(a.lastName()));
        order.setShippingAddress(orEmpty(a.address()));
        order.setShippingCity(orEmpty(a.city()));
        order.setShippingZipCode(orEmpty(a.zipCode()));
        order.setShippingCountry(orEmpty(a.country()));

        Instant now = Instant.now();
        order.setStatus("pending");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setEstimatedDelivery(now.plus(5, ChronoUnit.DAYS));
        order.setOrderNumber(generateOrderNumber());
        order.setTrackingNumber("TRK" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

        // 4) everything is valid: take the items off the shelf and save
        toReserve.forEach((product, qty) -> {
            if (product.getStock() != null) {
                product.setStock(product.getStock() - qty);
                productRepository.save(product);
            }
        });
        addEvent(order, "pending", "Order placed");
        Order saved = orderRepository.save(order);
        if (coupon != null) couponService.markUsed(coupon);
        cartService.removeProducts(user, quantities.keySet());   // bought items leave the cart, the rest stays
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(User actor, Long id) {
        Order order = findOrThrow(id);
        assertCanAccess(actor, order);
        return toDto(order);
    }

    @Transactional
    public OrderDto updateStatus(User actor, Long id, String newStatus) {
        Order order = findOrThrow(id);
        assertCanAccess(actor, order);

        String status = newStatus.trim().toLowerCase();
        if (!VALID_STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        String current = order.getStatus();
        if (status.equals(current)) {
            return toDto(order);                                   // nothing to do
        }

        if (!isAdmin(actor)) {
            // customers may only cancel their own order, and only before it ships
            if (!status.equals("cancelled")) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own orders");
            }
            if (!CUSTOMER_CAN_CANCEL.contains(current)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "This order can no longer be cancelled");
            }
        } else {
            if (current.equals("delivered") || current.equals("cancelled")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "This order is already " + current + " and can no longer be changed");
            }
            if (!status.equals("cancelled") && FLOW.indexOf(status) < FLOW.indexOf(current)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "An order cannot move back from " + current + " to " + status);
            }
        }

        if (status.equals("cancelled")) {
            restoreStock(order);
            couponService.release(order.getCouponCode());
        }

        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        addEvent(order, status, switch (status) {
            case "processing" -> "Your order is being prepared";
            case "shipped" -> "Handed over to the courier";
            case "delivered" -> "Delivered";
            case "cancelled" -> isAdmin(actor) ? "Cancelled by the store" : "Cancelled by you";
            default -> "Status updated";
        });
        return toDto(orderRepository.save(order));
    }

    // ---------------------------------------------------------------- returns

    /** Customer asks to return a delivered order (within 7 days of delivery). */
    @Transactional
    public OrderDto requestReturn(User actor, Long id, String reason) {
        Order order = findOrThrow(id);
        if (!order.getUser().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (!"delivered".equals(order.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only delivered orders can be returned");
        }
        if (order.getReturnStatus() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "A return was already " + order.getReturnStatus() + " for this order");
        }
        Instant deliveredAt = order.getEvents().stream()
                .filter(e -> "delivered".equals(e.getStatus()) && e.getNote() != null && e.getNote().equals("Delivered"))
                .map(OrderEvent::getCreatedAt).reduce((a, b) -> b)
                .orElse(order.getUpdatedAt());
        if (deliveredAt.plus(RETURN_WINDOW_DAYS, ChronoUnit.DAYS).isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The " + RETURN_WINDOW_DAYS + "-day return window for this order has passed");
        }
        order.setReturnStatus("requested");
        order.setReturnReason(reason.trim());
        order.setUpdatedAt(Instant.now());
        addEvent(order, "delivered", "Return requested: " + reason.trim());
        return toDto(orderRepository.save(order));
    }

    /** Admin approves (items go back on the shelf, order no longer counts as sales) or rejects a return. */
    @Transactional
    public OrderDto decideReturn(User actor, Long id, String decision) {
        if (!isAdmin(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the store can decide on returns");
        }
        Order order = findOrThrow(id);
        if (!"requested".equals(order.getReturnStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "There is no open return request for this order");
        }
        String d = decision.trim().toLowerCase();
        if (d.equals("approve")) {
            order.setReturnStatus("approved");
            restoreStock(order);
            addEvent(order, "delivered", "Return approved: refund of ₹" + order.getTotal().toPlainString() + " initiated");
        } else if (d.equals("reject")) {
            order.setReturnStatus("rejected");
            addEvent(order, "delivered", "Return request was declined");
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be approve or reject");
        }
        order.setUpdatedAt(Instant.now());
        return toDto(orderRepository.save(order));
    }

    private void addEvent(Order order, String status, String note) {
        OrderEvent e = new OrderEvent();
        e.setOrder(order);
        e.setStatus(status);
        e.setNote(note);
        e.setCreatedAt(Instant.now());
        order.getEvents().add(e);
    }

    // ---------------------------------------------------------------- helpers

    /** Cancelled orders give their items back to the shelf. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.lockById(item.getProductId()).ifPresent(p -> {   // product may have been deleted
                if (p.getStock() != null) {
                    p.setStock(p.getStock() + item.getQuantity());
                    productRepository.save(p);
                }
            });
        }
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void assertCanAccess(User actor, Order order) {
        if (!isAdmin(actor) && !order.getUser().getId().equals(actor.getId())) {
            // 404 instead of 403 so order ids of other people are not revealed
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    private boolean isAdmin(User user) {
        return "admin".equalsIgnoreCase(user.getRole());
    }

    private String generateOrderNumber() {
        String number;
        do {
            number = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        } while (orderRepository.existsByOrderNumber(number));
        return number;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** Orders placed before the timeline existed get a single "Order placed" line. */
    private List<OrderEventDto> history(Order o) {
        if (o.getEvents().isEmpty()) {
            return List.of(new OrderEventDto("pending", "Order placed", o.getCreatedAt()));
        }
        return o.getEvents().stream()
                .map(e -> new OrderEventDto(e.getStatus(), e.getNote(), e.getCreatedAt()))
                .toList();
    }

    private OrderDto toDto(Order o) {
        List<CartItemDto> items = o.getItems().stream()
                .map(i -> new CartItemDto(i.getProductId(), i.getName(), i.getPrice(), i.getImage(),
                        i.getCategory(), null, null, null, i.getQuantity()))
                .toList();

        AddressDto address = new AddressDto(o.getShippingFirstName(), o.getShippingLastName(),
                o.getShippingAddress(), o.getShippingCity(), o.getShippingZipCode(), o.getShippingCountry());

        return new OrderDto(
                String.valueOf(o.getId()),
                o.getOrderNumber(),
                String.valueOf(o.getUser().getId()),
                items,
                o.getSubtotal(), o.getDiscount(), o.getCouponCode(),
                o.getShipping(), o.getTax(), o.getTotal(),
                o.getStatus(),
                o.getPaymentMethod() == null ? "card" : o.getPaymentMethod(),
                address,
                o.getCreatedAt(), o.getUpdatedAt(), o.getEstimatedDelivery(),
                o.getTrackingNumber(),
                history(o),
                o.getReturnStatus(), o.getReturnReason());
    }
}
