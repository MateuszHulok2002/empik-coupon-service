package pl.empik.couponservice.coupon.api;

import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;

public record RedemptionResponse(
        UUID redemptionId,
        String couponCode,
        String userId,
        Instant redeemedAt,
        int currentUses,
        int remainingUses,
        String country) {

    static RedemptionResponse from(RedemptionResult result) {
        return new RedemptionResponse(
                result.redemptionId(),
                result.couponCode(),
                result.userId(),
                result.redeemedAt(),
                result.currentUses(),
                result.remainingUses(),
                result.country());
    }
}
