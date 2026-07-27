package pl.empik.couponservice.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.couponservice.coupon.application.port.out.CouponRedemptionRepository;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.couponservice.coupon.domain.exception.CouponExhaustedException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionTransactionTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");
    private static final CouponCode CODE = new CouponCode("WIOSNA");
    private static final UserId USER = new UserId("user-1");
    private static final CountryCode POLAND = new CountryCode("PL");

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponRedemptionRepository redemptionRepository;

    private CouponRedemptionTransaction transaction;

    @BeforeEach
    void setUp() {
        transaction =
                new CouponRedemptionTransaction(
                        couponRepository,
                        redemptionRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldFailWhenCouponDisappearedBeforeLockAcquisition() {
        when(couponRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaction.redeem(CODE, USER, POLAND))
                .isInstanceOf(CouponNotFoundException.class);

        verifyNoInteractions(redemptionRepository);
    }

    @Test
    void shouldRejectSecondRedemptionBySameUser() {
        Coupon coupon = couponWithState(10, 1);
        when(couponRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(coupon.id(), USER))
                .thenReturn(true);

        assertThatThrownBy(() -> transaction.redeem(CODE, USER, POLAND))
                .isInstanceOf(CouponAlreadyRedeemedException.class);

        assertThat(coupon.currentUses()).isEqualTo(1);
        verify(couponRepository, never()).update(coupon);
    }

    @Test
    void shouldRejectExhaustedCouponWithoutPersistingRedemption() {
        Coupon coupon = couponWithState(1, 1);
        when(couponRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(coupon.id(), USER))
                .thenReturn(false);

        assertThatThrownBy(() -> transaction.redeem(CODE, USER, POLAND))
                .isInstanceOf(CouponExhaustedException.class);

        verify(couponRepository, never()).update(coupon);
        verify(redemptionRepository, never())
                .save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIncrementCounterAndPersistRedemptionInOneTransaction() {
        Coupon coupon = couponWithState(10, 0);
        when(couponRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(coupon.id(), USER))
                .thenReturn(false);

        RedemptionResult result = transaction.redeem(CODE, USER, POLAND);

        assertThat(coupon.currentUses()).isEqualTo(1);
        assertThat(result.couponCode()).isEqualTo(CODE.value());
        assertThat(result.userId()).isEqualTo(USER.value());
        assertThat(result.redeemedAt()).isEqualTo(NOW);
        assertThat(result.currentUses()).isEqualTo(1);
        assertThat(result.remainingUses()).isEqualTo(9);

        verify(couponRepository).update(coupon);

        ArgumentCaptor<CouponRedemption> captor =
                ArgumentCaptor.forClass(CouponRedemption.class);
        verify(redemptionRepository).save(captor.capture(), org.mockito.ArgumentMatchers.eq(CODE));

        CouponRedemption redemption = captor.getValue();
        assertThat(redemption.couponId()).isEqualTo(coupon.id());
        assertThat(redemption.userId()).isEqualTo(USER);
        assertThat(redemption.redeemedAt()).isEqualTo(NOW);
        assertThat(redemption.resolvedCountry()).isEqualTo(POLAND);
    }

    private static Coupon couponWithState(int maxUses, int currentUses) {
        return Coupon.restore(
                UUID.randomUUID(),
                CODE,
                Instant.parse("2026-07-01T00:00:00Z"),
                maxUses,
                currentUses,
                POLAND);
    }
}
