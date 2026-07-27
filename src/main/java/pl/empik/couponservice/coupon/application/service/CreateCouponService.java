package pl.empik.couponservice.coupon.application.service;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.CreateCouponUseCase;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.result.CouponResult;
import pl.empik.couponservice.coupon.domain.model.Coupon;

@Service
public class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final Clock clock;

    public CreateCouponService(CouponRepository couponRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CouponResult create(CreateCouponCommand command) {
        Coupon coupon =
                Coupon.create(
                        command.code(), command.maxUses(), command.country(), clock.instant());

        couponRepository.saveNew(coupon);

        return CouponResult.from(coupon);
    }
}
