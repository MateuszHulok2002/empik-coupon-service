package pl.empik.couponservice.coupon.infrastructure.geo;

import java.util.Objects;
import pl.empik.couponservice.coupon.application.port.out.IpCountryResolver;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

final class FixedIpCountryResolver implements IpCountryResolver {

    private final CountryCode country;

    FixedIpCountryResolver(CountryCode country) {
        this.country = Objects.requireNonNull(country, "country");
    }

    @Override
    public CountryCode resolve(IpAddress ipAddress) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        return country;
    }
}
