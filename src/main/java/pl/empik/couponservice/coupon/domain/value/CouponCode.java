package pl.empik.couponservice.coupon.domain.value;

import java.util.Locale;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

public record CouponCode(String value) {

    public static final int MAX_LENGTH = 64;

    public CouponCode {
        if (value == null) {
            throw new InvalidRequestDataException("Coupon code must not be null.");
        }

        value = value.trim().toUpperCase(Locale.ROOT);

        if (value.isBlank()) {
            throw new InvalidRequestDataException("Coupon code must not be blank.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidRequestDataException(
                    "Coupon code must not exceed %d characters.".formatted(MAX_LENGTH));
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidRequestDataException("Coupon code must not contain control characters.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
