package pl.empik.couponservice.coupon.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

class CouponRedemptionTest {

    @Test
    void shouldCreateRedemption() {
        UUID couponId = UUID.randomUUID();
        Instant redeemedAt = Instant.parse("2026-07-24T08:00:00Z");

        CouponRedemption redemption =
                CouponRedemption.create(
                        couponId, new UserId("user-1"), redeemedAt, new CountryCode("PL"));

        assertThat(redemption.id()).isNotNull();
        assertThat(redemption.couponId()).isEqualTo(couponId);
        assertThat(redemption.userId().value()).isEqualTo("user-1");
        assertThat(redemption.redeemedAt()).isEqualTo(redeemedAt);
        assertThat(redemption.resolvedCountry().value()).isEqualTo("PL");
    }

    @Test
    void shouldRejectNullState() {
        assertThatThrownBy(
                        () ->
                                new CouponRedemption(
                                        null,
                                        UUID.randomUUID(),
                                        new UserId("user"),
                                        Instant.now(),
                                        new CountryCode("PL")))
                .isInstanceOf(NullPointerException.class);
    }
}
