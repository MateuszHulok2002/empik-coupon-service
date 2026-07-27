package pl.empik.couponservice.coupon.domain.exception;

public final class CouponAlreadyRedeemedException extends CouponServiceException {

    private static final long serialVersionUID = 1L;

    public CouponAlreadyRedeemedException(String code, String userId) {
        super(
                "COUPON_ALREADY_REDEEMED",
                "User '%s' has already redeemed coupon '%s'.".formatted(userId, code));
    }
}
