package pl.empik.couponservice.coupon.api;

import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.application.result.CouponResult;

public record CouponResponse(
        UUID id,
        String code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        int remainingUses,
        String country) {

    static CouponResponse from(CouponResult result) {
        return new CouponResponse(
                result.id(),
                result.code(),
                result.createdAt(),
                result.maxUses(),
                result.currentUses(),
                result.remainingUses(),
                result.country());
    }
}
