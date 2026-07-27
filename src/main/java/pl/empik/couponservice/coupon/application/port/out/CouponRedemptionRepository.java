package pl.empik.couponservice.coupon.application.port.out;

import java.util.UUID;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

public interface CouponRedemptionRepository {

    boolean existsByCouponIdAndUserId(UUID couponId, UserId userId);

    void save(CouponRedemption redemption, CouponCode couponCode);
}
