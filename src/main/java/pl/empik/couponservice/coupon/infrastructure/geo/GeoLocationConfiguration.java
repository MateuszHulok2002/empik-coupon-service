package pl.empik.couponservice.coupon.infrastructure.geo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import pl.empik.couponservice.coupon.application.port.out.IpCountryResolver;
import pl.empik.couponservice.coupon.domain.value.CountryCode;

@Configuration(proxyBeanMethods = false)
public class GeoLocationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.geo", name = "mode", havingValue = "remote", matchIfMissing = true)
    RestClient countryIsRestClient(
            RestClient.Builder restClientBuilder, GeoLocationProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toMilliseconds(properties.getConnectTimeout()));
        requestFactory.setReadTimeout(toMilliseconds(properties.getReadTimeout()));

        return restClientBuilder
                .baseUrl(requireHttpBaseUrl(properties.getBaseUrl()).toString())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "empik-coupon-service/1.0")
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.geo", name = "mode", havingValue = "remote", matchIfMissing = true)
    IpCountryResolver countryIsIpCountryResolver(
            RestClient countryIsRestClient, GeoLocationProperties properties) {
        Cache<String, CountryCode> cache =
                Caffeine.newBuilder()
                        .maximumSize(properties.getCacheMaximumSize())
                        .expireAfterWrite(requirePositive(properties.getCacheTtl(), "Cache TTL"))
                        .build();

        return new CountryIsIpCountryResolver(countryIsRestClient, cache);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.geo", name = "mode", havingValue = "fixed")
    IpCountryResolver fixedIpCountryResolver(GeoLocationProperties properties) {
        return new FixedIpCountryResolver(new CountryCode(properties.getFixedCountry()));
    }

    private static int toMilliseconds(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        long milliseconds = duration.toMillis();
        if (milliseconds <= 0 || milliseconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "HTTP timeout must be between 1 ms and %d ms."
                            .formatted(Integer.MAX_VALUE));
        }

        return (int) milliseconds;
    }

    private static Duration requirePositive(Duration duration, String propertyName) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero.");
        }
        return duration;
    }

    private static URI requireHttpBaseUrl(URI uri) {
        Objects.requireNonNull(uri, "uri");
        String scheme = uri.getScheme();
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);

        if (!(normalizedScheme.equals("http") || normalizedScheme.equals("https"))
                || uri.getHost() == null
                || uri.getHost().isBlank()
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Geolocation base URL must be an absolute HTTP(S) URL without user info or fragment.");
        }

        return uri;
    }
}
