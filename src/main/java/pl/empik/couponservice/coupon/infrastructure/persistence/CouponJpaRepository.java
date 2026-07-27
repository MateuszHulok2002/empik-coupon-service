package pl.empik.couponservice.coupon.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, UUID> {

    @Query("select c.countryCode from CouponJpaEntity c where c.code = :code")
    Optional<String> findCountryCodeByCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponJpaEntity c where c.code = :code")
    Optional<CouponJpaEntity> findByCodeForUpdate(@Param("code") String code);
}
