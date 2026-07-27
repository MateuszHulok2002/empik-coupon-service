package pl.empik.couponservice.coupon.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

public record CouponRedemption(
        UUID id,
        UUID couponId,
        UserId userId,
        Instant redeemedAt,
        CountryCode resolvedCountry) {

    public CouponRedemption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(couponId, "couponId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(redeemedAt, "redeemedAt");
        Objects.requireNonNull(resolvedCountry, "resolvedCountry");
    }

    public static CouponRedemption create(
            UUID couponId, UserId userId, Instant redeemedAt, CountryCode resolvedCountry) {
        return new CouponRedemption(
                UUID.randomUUID(), couponId, userId, redeemedAt, resolvedCountry);
    }
}
