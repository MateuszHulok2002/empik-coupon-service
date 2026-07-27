package pl.empik.couponservice.coupon.domain.exception;

public final class CouponCountryMismatchException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public CouponCountryMismatchException(String code, String expectedCountry, String actualCountry) {
        super(
                "COUPON_COUNTRY_MISMATCH",
                "Coupon '%s' is restricted to country %s and cannot be used from %s."
                        .formatted(code, expectedCountry, actualCountry));
    }
}
