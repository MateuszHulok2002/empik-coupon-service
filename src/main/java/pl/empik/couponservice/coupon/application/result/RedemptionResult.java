package pl.empik.couponservice.coupon.application.result;

import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;

public record RedemptionResult(
        UUID redemptionId,
        String couponCode,
        String userId,
        Instant redeemedAt,
        int currentUses,
        int remainingUses,
        String country) {

    public static RedemptionResult from(Coupon coupon, CouponRedemption redemption) {
        return new RedemptionResult(
                redemption.id(),
                coupon.code().value(),
                redemption.userId().value(),
                redemption.redeemedAt(),
                coupon.currentUses(),
                coupon.remainingUses(),
                redemption.resolvedCountry().value());
    }
}
