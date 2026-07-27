package pl.empik.couponservice.coupon.domain.value;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

public record CountryCode(String value) {

    private static final Set<String> ISO_COUNTRIES =
            Set.copyOf(Arrays.asList(Locale.getISOCountries()));

    public CountryCode {
        if (value == null) {
            throw new InvalidRequestDataException("Country code must not be null.");
        }

        value = value.trim().toUpperCase(Locale.ROOT);

        if (!ISO_COUNTRIES.contains(value)) {
            throw new InvalidRequestDataException(
                    "Country code must be a valid ISO 3166-1 alpha-2 code.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
