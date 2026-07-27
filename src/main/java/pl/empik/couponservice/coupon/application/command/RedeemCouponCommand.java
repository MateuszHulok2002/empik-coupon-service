package pl.empik.couponservice.coupon.application.command;

import java.util.Objects;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;
import pl.empik.couponservice.coupon.domain.value.UserId;

public record RedeemCouponCommand(CouponCode couponCode, UserId userId, IpAddress clientIp) {

    public RedeemCouponCommand {
        Objects.requireNonNull(couponCode, "couponCode");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(clientIp, "clientIp");
    }
}
