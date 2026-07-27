package pl.empik.couponservice.coupon.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.CreateCouponUseCase;
import pl.empik.couponservice.coupon.application.result.CouponResult;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;

@RestController
@RequestMapping(path = "/api/v1/coupons", produces = MediaType.APPLICATION_JSON_VALUE)
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;

    public CouponController(CreateCouponUseCase createCouponUseCase) {
        this.createCouponUseCase = createCouponUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        CouponResult result =
                createCouponUseCase.create(
                        new CreateCouponCommand(
                                new CouponCode(request.code()),
                                request.maxUses(),
                                new CountryCode(request.country())));

        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(result));
    }
}
