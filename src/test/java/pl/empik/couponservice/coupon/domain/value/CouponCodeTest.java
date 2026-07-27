package pl.empik.couponservice.coupon.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

class CouponCodeTest {

    @Test
    void shouldNormalizeCodeCaseInsensitively() {
        CouponCode code = new CouponCode("  wiosna_2026  ");

        assertThat(code.value()).isEqualTo("WIOSNA_2026");
        assertThat(code.toString()).isEqualTo("WIOSNA_2026");
    }

    @Test
    void shouldRejectNullCode() {
        assertThatThrownBy(() -> new CouponCode(null))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessage("Coupon code must not be null.");
    }

    @Test
    void shouldRejectBlankCode() {
        assertThatThrownBy(() -> new CouponCode("   "))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessage("Coupon code must not be blank.");
    }

    @Test
    void shouldRejectCodeLongerThanLimit() {
        assertThatThrownBy(() -> new CouponCode("a".repeat(CouponCode.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void shouldRejectControlCharacters() {
        assertThatThrownBy(() -> new CouponCode("SPRING\nSALE"))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessageContaining("control characters");
    }
}
