package pl.empik.couponservice.coupon.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

@Component
public class ServletClientIpExtractor {

    public IpAddress extract(HttpServletRequest request) {
        return new IpAddress(request.getRemoteAddr());
    }
}
