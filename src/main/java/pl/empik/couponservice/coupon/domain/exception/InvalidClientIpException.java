package pl.empik.couponservice.coupon.domain.exception;

public final class InvalidClientIpException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public InvalidClientIpException(String message) {
        super("INVALID_CLIENT_IP", message);
    }
}
