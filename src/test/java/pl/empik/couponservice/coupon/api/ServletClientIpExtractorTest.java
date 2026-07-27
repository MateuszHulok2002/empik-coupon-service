package pl.empik.couponservice.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ServletClientIpExtractorTest {

    @Test
    void shouldUseContainerResolvedRemoteAddressInsteadOfParsingHeadersItself() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "1.1.1.1");

        assertThat(new ServletClientIpExtractor().extract(request).value())
                .isEqualTo("8.8.8.8");
    }
}
