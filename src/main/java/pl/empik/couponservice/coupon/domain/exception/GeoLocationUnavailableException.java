package pl.empik.couponservice.coupon.domain.exception;

public final class GeoLocationUnavailableException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public GeoLocationUnavailableException(String message) {
        super("GEOLOCATION_UNAVAILABLE", message);
    }

    public GeoLocationUnavailableException(String message, Throwable cause) {
        super("GEOLOCATION_UNAVAILABLE", message, cause);
    }
}
