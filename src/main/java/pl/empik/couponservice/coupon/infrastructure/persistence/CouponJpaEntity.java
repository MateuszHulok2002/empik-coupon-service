package pl.empik.couponservice.coupon.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

@Entity
@Table(name = "coupons")
class CouponJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = CouponCode.MAX_LENGTH, updatable = false)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "max_uses", nullable = false, updatable = false)
    private int maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(name = "country_code", nullable = false, length = 2, updatable = false)
    private String countryCode;

    protected CouponJpaEntity() {}

    private CouponJpaEntity(
            UUID id,
            String code,
            Instant createdAt,
            int maxUses,
            int currentUses,
            String countryCode) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.maxUses = maxUses;
        this.currentUses = currentUses;
        this.countryCode = countryCode;
    }

    static CouponJpaEntity fromDomain(Coupon coupon) {
        return new CouponJpaEntity(
                coupon.id(),
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.country().value());
    }

    Coupon toDomain() {
        return Coupon.restore(
                id,
                new CouponCode(code),
                createdAt,
                maxUses,
                currentUses,
                new CountryCode(countryCode));
    }

    void updateCurrentUses(int value) {
        this.currentUses = value;
    }
}
