package pl.empik.couponservice.coupon.domain.value;

import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

public record UserId(String value) {

    public static final int MAX_LENGTH = 128;

    public UserId {
        if (value == null) {
            throw new InvalidRequestDataException("User identifier must not be null.");
        }

        value = value.trim();

        if (value.isBlank()) {
            throw new InvalidRequestDataException("User identifier must not be blank.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidRequestDataException(
                    "User identifier must not exceed %d characters.".formatted(MAX_LENGTH));
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidRequestDataException(
                    "User identifier must not contain control characters.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
