package pl.empik.couponservice.coupon.application.port.in;

import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;

public interface RedeemCouponUseCase {

    RedemptionResult redeem(RedeemCouponCommand command);
}
