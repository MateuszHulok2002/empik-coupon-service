package pl.empik.couponservice.coupon.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.RedeemCouponUseCase;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.UserId;

@RestController
@RequestMapping(path = "/api/v1/coupon-redemptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class CouponRedemptionController {

    private final RedeemCouponUseCase redeemCouponUseCase;
    private final ServletClientIpExtractor clientIpExtractor;

    public CouponRedemptionController(
            RedeemCouponUseCase redeemCouponUseCase,
            ServletClientIpExtractor clientIpExtractor) {
        this.redeemCouponUseCase = redeemCouponUseCase;
        this.clientIpExtractor = clientIpExtractor;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RedemptionResponse> redeem(
            @Valid @RequestBody RedeemCouponRequest request,
            HttpServletRequest servletRequest) {
        RedemptionResult result =
                redeemCouponUseCase.redeem(
                        new RedeemCouponCommand(
                                new CouponCode(request.couponCode()),
                                new UserId(request.userId()),
                                clientIpExtractor.extract(servletRequest)));

        return ResponseEntity.status(HttpStatus.CREATED).body(RedemptionResponse.from(result));
    }
}
