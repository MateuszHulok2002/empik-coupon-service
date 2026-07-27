package pl.empik.couponservice.coupon.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.port.out.IpCountryResolver;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;
import pl.empik.couponservice.coupon.domain.value.UserId;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionServiceTest {

    private static final CouponCode CODE = new CouponCode("WIOSNA");
    private static final UserId USER = new UserId("user-1");
    private static final IpAddress IP = new IpAddress("8.8.8.8");
    private static final CountryCode POLAND = new CountryCode("PL");
    private static final RedeemCouponCommand COMMAND =
            new RedeemCouponCommand(CODE, USER, IP);

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IpCountryResolver ipCountryResolver;

    @Mock
    private CouponRedemptionTransaction redemptionTransaction;

    private CouponRedemptionService service;

    @BeforeEach
    void setUp() {
        service =
                new CouponRedemptionService(
                        couponRepository, ipCountryResolver, redemptionTransaction);
    }

    @Test
    void shouldFailFastWhenCouponDoesNotExistAndSkipExternalCall() {
        when(couponRepository.findCountryByCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(COMMAND))
                .isInstanceOf(CouponNotFoundException.class);

        verify(ipCountryResolver, never()).resolve(IP);
        verify(redemptionTransaction, never()).redeem(CODE, USER, POLAND);
    }

    @Test
    void shouldRejectWrongCountryBeforeTakingDatabaseWriteLock() {
        CountryCode germany = new CountryCode("DE");
        when(couponRepository.findCountryByCode(CODE)).thenReturn(Optional.of(POLAND));
        when(ipCountryResolver.resolve(IP)).thenReturn(germany);

        assertThatThrownBy(() -> service.redeem(COMMAND))
                .isInstanceOf(CouponCountryMismatchException.class)
                .hasMessageContaining("PL")
                .hasMessageContaining("DE");

        verify(redemptionTransaction, never()).redeem(CODE, USER, germany);
    }

    @Test
    void shouldDelegateMatchingCountryToTransactionalComponent() {
        RedemptionResult expected =
                new RedemptionResult(
                        UUID.randomUUID(),
                        CODE.value(),
                        USER.value(),
                        Instant.parse("2026-07-24T08:00:00Z"),
                        1,
                        9,
                        POLAND.value());

        when(couponRepository.findCountryByCode(CODE)).thenReturn(Optional.of(POLAND));
        when(ipCountryResolver.resolve(IP)).thenReturn(POLAND);
        when(redemptionTransaction.redeem(CODE, USER, POLAND)).thenReturn(expected);

        RedemptionResult result = service.redeem(COMMAND);

        assertThat(result).isEqualTo(expected);
    }
}
