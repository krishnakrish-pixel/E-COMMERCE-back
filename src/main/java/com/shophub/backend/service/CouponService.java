package com.shophub.backend.service;

import com.shophub.backend.dto.CouponDto;
import com.shophub.backend.dto.CouponRequest;
import com.shophub.backend.dto.CouponResult;
import com.shophub.backend.entity.Coupon;
import com.shophub.backend.exception.ApiException;
import com.shophub.backend.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class CouponService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    // ------------------------------------------------------------ customers

    /** Used by the checkout page to preview the discount. Nothing is consumed here. */
    @Transactional(readOnly = true)
    public CouponResult validate(String code, BigDecimal subtotal) {
        Coupon coupon = requireUsable(code, subtotal);
        return new CouponResult(coupon.getCode(), coupon.getDescription(), coupon.getDiscountType(),
                coupon.getDiscountValue(), discountFor(coupon, subtotal));
    }

    /** Throws a 400 with a customer-friendly message if the coupon cannot be used for this subtotal. */
    public Coupon requireUsable(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a coupon code");
        }
        Coupon c = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That coupon code is not valid"));

        if (!c.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This coupon is no longer active");
        }
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This coupon has expired");
        }
        if (c.getMaxUses() != null && c.getUsedCount() >= c.getMaxUses()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This coupon has reached its usage limit");
        }
        if (subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            BigDecimal more = c.getMinOrderAmount().subtract(subtotal).setScale(2, RoundingMode.HALF_UP);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add ₹" + more.toPlainString() + " more to use this coupon");
        }
        return c;
    }

    public BigDecimal discountFor(Coupon c, BigDecimal subtotal) {
        BigDecimal discount = Coupon.PERCENT.equals(c.getDiscountType())
                ? subtotal.multiply(c.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        if (discount.compareTo(subtotal) > 0) discount = subtotal;     // never go below zero
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void markUsed(Coupon c) {
        c.setUsedCount(c.getUsedCount() + 1);
        couponRepository.save(c);
    }

    /** Called when an order that used a coupon is cancelled. */
    @Transactional
    public void release(String code) {
        if (code == null || code.isBlank()) return;
        couponRepository.findByCodeIgnoreCase(code).ifPresent(c -> {
            c.setUsedCount(Math.max(0, c.getUsedCount() - 1));
            couponRepository.save(c);
        });
    }

    // ------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return couponRepository.findAll().stream()
                .sorted(Comparator.comparing(Coupon::getId).reversed())
                .map(CouponService::toDto)
                .toList();
    }

    @Transactional
    public CouponDto create(CouponRequest req) {
        String code = req.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A coupon with this code already exists");
        }
        Coupon c = new Coupon();
        c.setCode(code);
        apply(c, req);
        return toDto(couponRepository.save(c));
    }

    @Transactional
    public CouponDto update(Long id, CouponRequest req) {
        Coupon c = find(id);
        String code = req.code().trim().toUpperCase();
        if (!code.equals(c.getCode()) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A coupon with this code already exists");
        }
        c.setCode(code);
        apply(c, req);
        return toDto(couponRepository.save(c));
    }

    @Transactional
    public CouponDto setActive(Long id, boolean active) {
        Coupon c = find(id);
        c.setActive(active);
        return toDto(couponRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        couponRepository.delete(find(id));    // old orders only keep the code as text, so this is safe
    }

    private Coupon find(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coupon not found"));
    }

    private void apply(Coupon c, CouponRequest req) {
        String type = req.discountType().trim().toUpperCase();
        if (Coupon.PERCENT.equals(type) && req.discountValue().compareTo(HUNDRED) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A percentage discount cannot be more than 100");
        }
        if (req.maxUses() != null && req.maxUses() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Usage limit must be at least 1 (or empty for unlimited)");
        }
        c.setDescription(req.description() == null ? "" : req.description().trim());
        c.setDiscountType(type);
        c.setDiscountValue(req.discountValue());
        c.setMinOrderAmount(req.minOrderAmount() == null ? BigDecimal.ZERO : req.minOrderAmount());
        c.setMaxUses(req.maxUses());
        c.setExpiresAt(req.expiresAt());
        if (req.active() != null) c.setActive(req.active());
    }

    public static CouponDto toDto(Coupon c) {
        return new CouponDto(String.valueOf(c.getId()), c.getCode(), c.getDescription(), c.getDiscountType(),
                c.getDiscountValue(), c.getMinOrderAmount(), c.getMaxUses(), c.getUsedCount(),
                c.getExpiresAt(), c.isActive());
    }
}
