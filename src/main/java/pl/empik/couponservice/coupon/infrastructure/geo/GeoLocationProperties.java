package pl.empik.couponservice.coupon.infrastructure.geo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.geo")
public class GeoLocationProperties {

    public enum Mode {
        REMOTE,
        FIXED
    }

    @NotNull
    private Mode mode = Mode.REMOTE;

    @NotNull
    private URI baseUrl = URI.create("https://api.country.is");

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(1);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(2);

    @NotNull
    private Duration cacheTtl = Duration.ofHours(24);

    @Min(1)
    private long cacheMaximumSize = 10_000;

    @NotBlank
    private String fixedCountry = "PL";

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public long getCacheMaximumSize() {
        return cacheMaximumSize;
    }

    public void setCacheMaximumSize(long cacheMaximumSize) {
        this.cacheMaximumSize = cacheMaximumSize;
    }

    public String getFixedCountry() {
        return fixedCountry;
    }

    public void setFixedCountry(String fixedCountry) {
        this.fixedCountry = fixedCountry;
    }
}
