package pl.empik.couponservice.coupon.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CouponRedemptionJpaRepository
        extends JpaRepository<CouponRedemptionJpaEntity, UUID> {

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
