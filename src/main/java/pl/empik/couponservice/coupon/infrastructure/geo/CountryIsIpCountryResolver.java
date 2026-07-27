package pl.empik.couponservice.coupon.infrastructure.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.Objects;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.empik.couponservice.coupon.application.port.out.IpCountryResolver;
import pl.empik.couponservice.coupon.domain.exception.GeoLocationUnavailableException;
import pl.empik.couponservice.coupon.domain.exception.InvalidClientIpException;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

final class CountryIsIpCountryResolver implements IpCountryResolver {

    private final RestClient restClient;
    private final Cache<String, CountryCode> cache;

    CountryIsIpCountryResolver(RestClient restClient, Cache<String, CountryCode> cache) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    @Override
    public CountryCode resolve(IpAddress ipAddress) {
        if (!ipAddress.isPubliclyRoutable()) {
            throw new InvalidClientIpException(
                    "Client IP address must be publicly routable for geolocation.");
        }

        return cache.get(ipAddress.value(), ignored -> fetchCountry(ipAddress));
    }

    private CountryCode fetchCountry(IpAddress ipAddress) {
        try {
            CountryIsResponse response =
                    restClient
                            .get()
                            .uri("/{ip}", ipAddress.value())
                            .retrieve()
                            .body(CountryIsResponse.class);

            if (response == null || response.country() == null || response.country().isBlank()) {
                throw new GeoLocationUnavailableException(
                        "Geolocation provider returned no country for the client IP.");
            }

            try {
                return new CountryCode(response.country());
            } catch (InvalidRequestDataException exception) {
                throw new GeoLocationUnavailableException(
                        "Geolocation provider returned an unsupported country code.", exception);
            }
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new GeoLocationUnavailableException(
                    "Unable to resolve the client country at this time.", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CountryIsResponse(String ip, String country) {}
}
