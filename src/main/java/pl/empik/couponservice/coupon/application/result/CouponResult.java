package pl.empik.couponservice.coupon.application.result;

import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.model.Coupon;

public record CouponResult(
        UUID id,
        String code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        int remainingUses,
        String country) {

    public static CouponResult from(Coupon coupon) {
        return new CouponResult(
                coupon.id(),
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.remainingUses(),
                coupon.country().value());
    }
}
