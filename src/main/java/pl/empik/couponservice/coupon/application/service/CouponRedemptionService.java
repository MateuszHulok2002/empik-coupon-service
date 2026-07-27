package pl.empik.couponservice.coupon.application.service;

import org.springframework.stereotype.Service;
import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.RedeemCouponUseCase;
import pl.empik.couponservice.coupon.application.port.out.CouponRepository;
import pl.empik.couponservice.coupon.application.port.out.IpCountryResolver;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;

@Service
public class CouponRedemptionService implements RedeemCouponUseCase {

    private final CouponRepository couponRepository;
    private final IpCountryResolver ipCountryResolver;
    private final CouponRedemptionTransaction redemptionTransaction;

    public CouponRedemptionService(
            CouponRepository couponRepository,
            IpCountryResolver ipCountryResolver,
            CouponRedemptionTransaction redemptionTransaction) {
        this.couponRepository = couponRepository;
        this.ipCountryResolver = ipCountryResolver;
        this.redemptionTransaction = redemptionTransaction;
    }

    @Override
    public RedemptionResult redeem(RedeemCouponCommand command) {
        CountryCode expectedCountry =
                couponRepository
                        .findCountryByCode(command.couponCode())
                        .orElseThrow(
                                () ->
                                        new CouponNotFoundException(
                                                command.couponCode().value()));

        // Remote geolocation deliberately happens before opening a database transaction.
        CountryCode actualCountry = ipCountryResolver.resolve(command.clientIp());

        if (!expectedCountry.equals(actualCountry)) {
            throw new CouponCountryMismatchException(
                    command.couponCode().value(),
                    expectedCountry.value(),
                    actualCountry.value());
        }

        return redemptionTransaction.redeem(
                command.couponCode(), command.userId(), actualCountry);
    }
}
