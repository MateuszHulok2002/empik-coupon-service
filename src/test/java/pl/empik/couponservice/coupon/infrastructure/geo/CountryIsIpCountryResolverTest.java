package pl.empik.couponservice.coupon.infrastructure.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.empik.couponservice.coupon.domain.exception.GeoLocationUnavailableException;
import pl.empik.couponservice.coupon.domain.exception.InvalidClientIpException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

class CountryIsIpCountryResolverTest {

    private MockRestServiceServer server;
    private CountryIsIpCountryResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.country.is");
        server = MockRestServiceServer.bindTo(builder).build();
        resolver =
                new CountryIsIpCountryResolver(
                        builder.build(), Caffeine.newBuilder().maximumSize(100).build());
    }

    @Test
    void shouldResolveAndCacheCountry() {
        server.expect(requestTo("https://api.country.is/8.8.8.8"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                {"ip":"8.8.8.8","country":"US"}
                                """,
                                MediaType.APPLICATION_JSON));

        CountryCode first = resolver.resolve(new IpAddress("8.8.8.8"));
        CountryCode second = resolver.resolve(new IpAddress("8.8.8.8"));

        assertThat(first.value()).isEqualTo("US");
        assertThat(second).isEqualTo(first);
        server.verify();
    }

    @Test
    void shouldRejectPrivateIpWithoutCallingProvider() {
        assertThatThrownBy(() -> resolver.resolve(new IpAddress("127.0.0.1")))
                .isInstanceOf(InvalidClientIpException.class);

        server.verify();
    }

    @Test
    void shouldNotCacheProviderFailures() {
        server.expect(requestTo("https://api.country.is/1.0.0.1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo("https://api.country.is/1.0.0.1"))
                .andRespond(
                        withSuccess(
                                """
                                {"ip":"1.0.0.1","country":"AU"}
                                """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(new IpAddress("1.0.0.1")))
                .isInstanceOf(GeoLocationUnavailableException.class);

        assertThat(resolver.resolve(new IpAddress("1.0.0.1")).value()).isEqualTo("AU");
        server.verify();
    }

    @Test
    void shouldTranslateHttpErrorToDomainException() {
        server.expect(requestTo("https://api.country.is/1.1.1.1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> resolver.resolve(new IpAddress("1.1.1.1")))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("Unable to resolve");

        server.verify();
    }

    @Test
    void shouldTranslateMalformedProviderResponseToDomainException() {
        server.expect(requestTo("https://api.country.is/8.8.4.4"))
                .andRespond(withSuccess("{not-json}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(new IpAddress("8.8.4.4")))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("Unable to resolve");

        server.verify();
    }

    @Test
    void shouldRejectEmptyCountryResponse() {
        server.expect(requestTo("https://api.country.is/8.8.4.4"))
                .andRespond(
                        withSuccess(
                                """
                                {"ip":"8.8.4.4","country":null}
                                """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(new IpAddress("8.8.4.4")))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("no country");

        server.verify();
    }

    @Test
    void shouldRejectUnsupportedCountryReturnedByProvider() {
        server.expect(requestTo("https://api.country.is/9.9.9.9"))
                .andRespond(
                        withSuccess(
                                """
                                {"ip":"9.9.9.9","country":"ZZ"}
                                """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(new IpAddress("9.9.9.9")))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("unsupported country");

        server.verify();
    }
}
