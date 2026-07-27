package pl.empik.couponservice.coupon.domain.exception;

public abstract class CouponServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    protected CouponServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected CouponServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
