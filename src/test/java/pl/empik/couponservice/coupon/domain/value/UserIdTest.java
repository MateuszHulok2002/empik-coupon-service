package pl.empik.couponservice.coupon.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

class UserIdTest {

    @Test
    void shouldTrimUserIdentifierWithoutChangingItsCase() {
        UserId userId = new UserId("  Customer-ABC  ");

        assertThat(userId.value()).isEqualTo("Customer-ABC");
        assertThat(userId.toString()).isEqualTo("Customer-ABC");
    }

    @Test
    void shouldRejectNullOrBlankIdentifier() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(InvalidRequestDataException.class);
        assertThatThrownBy(() -> new UserId(" "))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    void shouldRejectIdentifierLongerThanLimit() {
        assertThatThrownBy(() -> new UserId("x".repeat(UserId.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessageContaining("must not exceed");
    }

    @Test
    void shouldRejectControlCharacters() {
        assertThatThrownBy(() -> new UserId("user\tid"))
                .isInstanceOf(InvalidRequestDataException.class)
                .hasMessageContaining("control characters");
    }
}
