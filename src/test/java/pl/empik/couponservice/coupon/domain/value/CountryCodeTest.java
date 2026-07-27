package pl.empik.couponservice.coupon.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

class CountryCodeTest {

    @Test
    void shouldNormalizeValidIsoCountryCode() {
        CountryCode country = new CountryCode(" pl ");

        assertThat(country.value()).isEqualTo("PL");
        assertThat(country.toString()).isEqualTo("PL");
    }

    @Test
    void shouldRejectUnknownCountryCode() {
        assertThatThrownBy(() -> new CountryCode("ZZ"))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    void shouldRejectNullCountryCode() {
        assertThatThrownBy(() -> new CountryCode(null))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessage("Country code must not be null.");
    }
}
