package pl.empik.couponservice.coupon.domain.exception;

public final class CouponAlreadyExistsException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public CouponAlreadyExistsException(String code) {
        super("COUPON_ALREADY_EXISTS", "Coupon with code '%s' already exists.".formatted(code));
    }
}
