package pl.empik.couponservice.coupon.infrastructure.persistence;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import pl.empik.couponservice.coupon.application.port.out.CouponRedemptionRepository;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

@Repository
class CouponRedemptionPersistenceAdapter implements CouponRedemptionRepository {

    private static final String UNIQUE_REDEMPTION_CONSTRAINT =
            "uk_coupon_redemptions_coupon_user";

    private final CouponRedemptionJpaRepository repository;

    CouponRedemptionPersistenceAdapter(CouponRedemptionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByCouponIdAndUserId(UUID couponId, UserId userId) {
        return repository.existsByCouponIdAndUserId(couponId, userId.value());
    }

    @Override
    public void save(CouponRedemption redemption, CouponCode couponCode) {
        try {
            repository.saveAndFlush(CouponRedemptionJpaEntity.fromDomain(redemption));
        } catch (DataIntegrityViolationException exception) {
            if (DatabaseConstraintInspector.containsConstraint(
                    exception, UNIQUE_REDEMPTION_CONSTRAINT)) {
                throw new CouponAlreadyRedeemedException(
                        couponCode.value(), redemption.userId().value());
            }
            throw exception;
        }
    }
}
