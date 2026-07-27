package pl.empik.couponservice.coupon.domain.exception;

public final class CouponNotFoundException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public CouponNotFoundException(String code) {
        super("COUPON_NOT_FOUND", "Coupon with code '%s' does not exist.".formatted(code));
    }
}
