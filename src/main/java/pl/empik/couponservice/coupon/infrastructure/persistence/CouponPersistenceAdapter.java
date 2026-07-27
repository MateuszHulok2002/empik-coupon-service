package pl.empik.couponservice.coupon.infrastructure.persistence;

import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyExistsException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.model.Coupon;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

@Repository
class CouponPersistenceAdapter implements CouponRepository {

    private static final String UNIQUE_CODE_CONSTRAINT = "uk_coupons_code";

    private final CouponJpaRepository repository;

    CouponPersistenceAdapter(CouponJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveNew(Coupon coupon) {
        try {
            repository.saveAndFlush(CouponJpaEntity.fromDomain(coupon));
        } catch (DataIntegrityViolationException exception) {
            if (DatabaseConstraintInspector.containsConstraint(
                    exception, UNIQUE_CODE_CONSTRAINT)) {
                throw new CouponAlreadyExistsException(coupon.code().value());
            }
            throw exception;
        }
    }

    @Override
    public Optional<CountryCode> findCountryByCode(CouponCode code) {
        return repository
                .findCountryCodeByCode(code.value())
                .map(CountryCode::new);
    }

    @Override
    public Optional<Coupon> findByCodeForUpdate(CouponCode code) {
        return repository
                .findByCodeForUpdate(code.value())
                .map(CouponJpaEntity::toDomain);
    }

    @Override
    public void update(Coupon coupon) {
        // The preceding findByCodeForUpdate call loaded this entity into the current persistence
        // context. findById therefore resolves the same managed instance from the first-level cache;
        // synchronizing the mutable counter is persisted by dirty checking on transaction commit.
        CouponJpaEntity entity =
                repository
                        .findById(coupon.id())
                        .orElseThrow(
                                () -> new CouponNotFoundException(coupon.code().value()));

        entity.updateCurrentUses(coupon.currentUses());
    }
}
