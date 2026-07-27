package pl.empik.couponservice.coupon.domain.exception;

public final class InvalidRequestDataException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestDataException(String message) {
        super("INVALID_REQUEST_DATA", message);
    }
}
