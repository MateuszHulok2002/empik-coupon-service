package pl.empik.couponservice.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.result.CouponResult;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

@ExtendWith(MockitoExtension.class)
class CreateCouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");

    @Mock
    private CouponRepository couponRepository;

    @Test
    void shouldCreateAndPersistCoupon() {
        CreateCouponService service =
                new CreateCouponService(
                        couponRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        CouponResult result =
                service.create(
                        new CreateCouponCommand(
                                new CouponCode("wiosna"), 25, new CountryCode("pl")));

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).saveNew(captor.capture());

        Coupon persisted = captor.getValue();
        assertThat(persisted.code().value()).isEqualTo("WIOSNA");
        assertThat(persisted.createdAt()).isEqualTo(NOW);
        assertThat(persisted.maxUses()).isEqualTo(25);
        assertThat(persisted.currentUses()).isZero();
        assertThat(persisted.country().value()).isEqualTo("PL");

        assertThat(result.id()).isEqualTo(persisted.id());
        assertThat(result.code()).isEqualTo("WIOSNA");
        assertThat(result.currentUses()).isZero();
        assertThat(result.remainingUses()).isEqualTo(25);
    }
}
