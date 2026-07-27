package pl.empik.couponservice.coupon.application.command;

import java.util.Objects;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

public record CreateCouponCommand(CouponCode code, int maxUses, CountryCode country) {

    public CreateCouponCommand {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(country, "country");

        if (maxUses <= 0) {
            throw new InvalidRequestDataException("Maximum number of uses must be greater than zero.");
        }
    }
}
