package pl.empik.couponservice.coupon.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

public record RedeemCouponRequest(
        @NotBlank @Size(max = CouponCode.MAX_LENGTH) String couponCode,
        @NotBlank @Size(max = UserId.MAX_LENGTH) String userId) {}
