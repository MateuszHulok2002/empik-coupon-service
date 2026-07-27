package pl.empik.couponservice.coupon.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.model.CouponRedemption;
import pl.empik.couponservice.coupon.domain.value.UserId;

@Entity
@Table(name = "coupon_redemptions")
class CouponRedemptionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private UUID couponId;

    @Column(name = "user_id", nullable = false, length = UserId.MAX_LENGTH, updatable = false)
    private String userId;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    @Column(name = "resolved_country_code", nullable = false, length = 2, updatable = false)
    private String resolvedCountryCode;

    protected CouponRedemptionJpaEntity() {}

    private CouponRedemptionJpaEntity(
            UUID id,
            UUID couponId,
            String userId,
            Instant redeemedAt,
            String resolvedCountryCode) {
        this.id = id;
        this.couponId = couponId;
        this.userId = userId;
        this.redeemedAt = redeemedAt;
        this.resolvedCountryCode = resolvedCountryCode;
    }

    static CouponRedemptionJpaEntity fromDomain(CouponRedemption redemption) {
        return new CouponRedemptionJpaEntity(
                redemption.id(),
                redemption.couponId(),
                redemption.userId().value(),
                redemption.redeemedAt(),
                redemption.resolvedCountry().value());
    }
}
