package pl.empik.couponservice.coupon.domain.exception;

public final class CouponExhaustedException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public CouponExhaustedException(String code) {
        super("COUPON_EXHAUSTED", "Coupon with code '%s' has reached its usage limit.".formatted(code));
    }
}
