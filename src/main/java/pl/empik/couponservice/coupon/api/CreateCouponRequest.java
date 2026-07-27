package pl.empik.couponservice.coupon.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

public record CreateCouponRequest(
        @NotBlank @Size(max = CouponCode.MAX_LENGTH) String code,
        @NotNull @Positive Integer maxUses,
        @NotBlank @Size(min = 2, max = 2) String country) {}
