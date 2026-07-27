package pl.empik.couponservice.coupon.application.service;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.empik.couponservice.coupon.application.port.out.CouponRedemptionRepository;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

@Service
public class CouponRedemptionTransaction {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final Clock clock;

    public CouponRedemptionTransaction(
            CouponRepository couponRepository,
            CouponRedemptionRepository redemptionRepository,
            Clock clock) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
        this.clock = clock;
    }

    @Transactional
    public RedemptionResult redeem(
            CouponCode couponCode, UserId userId, CountryCode requesterCountry) {
        Coupon coupon =
                couponRepository
                        .findByCodeForUpdate(couponCode)
                        .orElseThrow(() -> new CouponNotFoundException(couponCode.value()));

        if (redemptionRepository.existsByCouponIdAndUserId(coupon.id(), userId)) {
            throw new CouponAlreadyRedeemedException(couponCode.value(), userId.value());
        }

        coupon.registerRedemption(requesterCountry);

        CouponRedemption redemption =
                CouponRedemption.create(
                        coupon.id(), userId, clock.instant(), requesterCountry);

        couponRepository.update(coupon);
        redemptionRepository.save(redemption, couponCode);

        return RedemptionResult.from(coupon, redemption);
    }
}
