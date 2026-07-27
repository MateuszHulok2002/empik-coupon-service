package pl.empik.couponservice.coupon.application.port.out;

import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

public interface IpCountryResolver {

    CountryCode resolve(IpAddress ipAddress);
}
