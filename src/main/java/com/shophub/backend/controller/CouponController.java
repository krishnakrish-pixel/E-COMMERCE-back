package com.shophub.backend.controller;

import com.shophub.backend.dto.*;
import com.shophub.backend.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** /validate is open to any signed-in customer. Everything else is admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public CouponResult validate(@Valid @RequestBody CouponValidateRequest request) {
        return couponService.validate(request.code(), request.subtotal());
    }

    @GetMapping
    public List<CouponDto> list() {
        return couponService.list();
    }

    @PostMapping
    public ResponseEntity<CouponDto> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @PutMapping("/{id}")
    public CouponDto update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return couponService.update(id, request);
    }

    /** Quick enable / disable switch: body {"active": true|false} */
    @PutMapping("/{id}/active")
    public CouponDto setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return couponService.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
