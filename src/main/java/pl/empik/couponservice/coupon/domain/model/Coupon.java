package pl.empik.couponservice.coupon.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponExhaustedException;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

public final class Coupon {

    private final UUID id;
    private final CouponCode code;
    private final Instant createdAt;
    private final int maxUses;
    private int currentUses;
    private final CountryCode country;

    private Coupon(
            UUID id,
            CouponCode code,
            Instant createdAt,
            int maxUses,
            int currentUses,
            CountryCode country) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.country = Objects.requireNonNull(country, "country");

        if (maxUses <= 0) {
            throw new InvalidRequestDataException("Maximum number of uses must be greater than zero.");
        }
        if (currentUses < 0 || currentUses > maxUses) {
            throw new InvalidRequestDataException(
                    "Current uses must be between zero and the maximum number of uses.");
        }

        this.maxUses = maxUses;
        this.currentUses = currentUses;
    }

    public static Coupon create(
            CouponCode code, int maxUses, CountryCode country, Instant createdAt) {
        return new Coupon(UUID.randomUUID(), code, createdAt, maxUses, 0, country);
    }

    public static Coupon restore(
            UUID id,
            CouponCode code,
            Instant createdAt,
            int maxUses,
            int currentUses,
            CountryCode country) {
        return new Coupon(id, code, createdAt, maxUses, currentUses, country);
    }

    public void registerRedemption(CountryCode requesterCountry) {
        if (!country.equals(requesterCountry)) {
            throw new CouponCountryMismatchException(
                    code.value(), country.value(), requesterCountry.value());
        }
        if (currentUses >= maxUses) {
            throw new CouponExhaustedException(code.value());
        }

        currentUses++;
    }

    public int remainingUses() {
        return maxUses - currentUses;
    }

    public UUID id() {
        return id;
    }

    public CouponCode code() {
        return code;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int maxUses() {
        return maxUses;
    }

    public int currentUses() {
        return currentUses;
    }

    public CountryCode country() {
        return country;
    }
}
