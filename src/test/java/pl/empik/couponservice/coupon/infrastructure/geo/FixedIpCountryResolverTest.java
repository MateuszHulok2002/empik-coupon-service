package pl.empik.couponservice.coupon.infrastructure.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

class FixedIpCountryResolverTest {

    @Test
    void shouldReturnConfiguredCountryEvenForLocalhost() {
        FixedIpCountryResolver resolver =
                new FixedIpCountryResolver(new CountryCode("PL"));

        assertThat(resolver.resolve(new IpAddress("127.0.0.1")).value()).isEqualTo("PL");
    }
}
