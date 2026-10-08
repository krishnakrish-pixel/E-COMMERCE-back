package com.shophub.backend.controller;

import com.shophub.backend.dto.OrderDto;
import com.shophub.backend.dto.OrderRequest;
import com.shophub.backend.dto.ReturnDecisionRequest;
import com.shophub.backend.dto.ReturnRequest;
import com.shophub.backend.dto.StatusRequest;
import com.shophub.backend.entity.User;
import com.shophub.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Place an order. Prices / totals are calculated on the server. */
    @PostMapping
    public ResponseEntity<OrderDto> create(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(user, request));
    }

    /** Orders of the logged-in user. */
    @GetMapping("/my")
    public List<OrderDto> myOrders(@AuthenticationPrincipal User user) {
        return orderService.getMyOrders(user);
    }

    /** All orders - admin only (see SecurityConfig). */
    @GetMapping
    public List<OrderDto> allOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return orderService.getOrder(user, id);
    }

    /** Customer: ask to return a delivered order (within 7 days). */
    @PostMapping("/{id}/return")
    public OrderDto requestReturn(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ReturnRequest request) {
        return orderService.requestReturn(user, id, request.reason());
    }

    /** Admin: approve or reject a return request. */
    @PutMapping("/{id}/return")
    public OrderDto decideReturn(@AuthenticationPrincipal User user,
                                 @PathVariable Long id,
                                 @Valid @RequestBody ReturnDecisionRequest request) {
        return orderService.decideReturn(user, id, request.decision());
    }

    /** Admin: any status. Customer: can only cancel own pending / processing order. */
    @PutMapping("/{id}/status")
    public OrderDto updateStatus(@AuthenticationPrincipal User user,
                                 @PathVariable Long id,
                                 @Valid @RequestBody StatusRequest request) {
        return orderService.updateStatus(user, id, request.status());
    }
}
