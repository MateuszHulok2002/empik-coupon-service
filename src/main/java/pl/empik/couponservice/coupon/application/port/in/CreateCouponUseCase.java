package pl.empik.couponservice.coupon.application.port.in;

import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.result.CouponResult;

public interface CreateCouponUseCase {

    CouponResult create(CreateCouponCommand command);
}
