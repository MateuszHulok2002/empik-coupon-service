package pl.empik.couponservice.coupon.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponExhaustedException;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");
    private static final CouponCode CODE = new CouponCode("WIOSNA");
    private static final CountryCode POLAND = new CountryCode("PL");

    @Test
    void shouldCreateCouponWithZeroUses() {
        Coupon coupon = Coupon.create(CODE, 2, POLAND, NOW);

        assertThat(coupon.id()).isNotNull();
        assertThat(coupon.code()).isEqualTo(CODE);
        assertThat(coupon.createdAt()).isEqualTo(NOW);
        assertThat(coupon.maxUses()).isEqualTo(2);
        assertThat(coupon.currentUses()).isZero();
        assertThat(coupon.remainingUses()).isEqualTo(2);
        assertThat(coupon.country()).isEqualTo(POLAND);
    }

    @Test
    void shouldRegisterRedemptionForMatchingCountry() {
        Coupon coupon = Coupon.create(CODE, 2, POLAND, NOW);

        coupon.registerRedemption(POLAND);

        assertThat(coupon.currentUses()).isEqualTo(1);
        assertThat(coupon.remainingUses()).isEqualTo(1);
    }

    @Test
    void shouldRejectRedemptionFromDifferentCountryWithoutChangingCounter() {
        Coupon coupon = Coupon.create(CODE, 2, POLAND, NOW);

        assertThatThrownBy(() -> coupon.registerRedemption(new CountryCode("DE")))
                .isInstanceOf(CouponCountryMismatchException.class)
                .hasMessageContaining("restricted to country PL");

        assertThat(coupon.currentUses()).isZero();
    }

    @Test
    void shouldRejectRedemptionAfterLimitIsReached() {
        Coupon coupon = Coupon.restore(UUID.randomUUID(), CODE, NOW, 1, 1, POLAND);

        assertThatThrownBy(() -> coupon.registerRedemption(POLAND))
                .isInstanceOf(CouponExhaustedException.class);

        assertThat(coupon.currentUses()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidRestoredState() {
        assertThatThrownBy(
                        () ->
                                Coupon.restore(
                                        UUID.randomUUID(), CODE, NOW, 0, 0, POLAND))
                .isInstanceOf(InvalidRequestDataException.class);

        assertThatThrownBy(
                        () ->
                                Coupon.restore(
                                        UUID.randomUUID(), CODE, NOW, 2, 3, POLAND))
                .isInstanceOf(InvalidRequestDataException.class);
    }
}
