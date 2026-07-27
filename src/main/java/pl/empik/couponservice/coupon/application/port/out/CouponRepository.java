package pl.empik.couponservice.coupon.application.port.out;

import java.util.Optional;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

public interface CouponRepository {

    void saveNew(Coupon coupon);

    Optional<CountryCode> findCountryByCode(CouponCode code);

    Optional<Coupon> findByCodeForUpdate(CouponCode code);

    void update(Coupon coupon);
}
