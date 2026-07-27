package pl.empik.couponservice.coupon.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.empik.couponservice.coupon.domain.exception.InvalidClientIpException;

class IpAddressTest {

    @Test
    void shouldAcceptAndNormalizeIpv4AddressWithoutNameResolution() {
        IpAddress address = new IpAddress("8.8.8.8");

        assertThat(address.value()).isEqualTo("8.8.8.8");
        assertThat(address.toString()).isEqualTo("8.8.8.8");
        assertThat(address.isPubliclyRoutable()).isTrue();
    }

    @Test
    void shouldAcceptBracketedIpv6AndRemoveZoneIdentifier() {
        IpAddress address = new IpAddress("[2001:4860:4860::8888]");

        assertThat(address.value()).contains(":").doesNotContain("[").doesNotContain("]");
        assertThat(address.isPubliclyRoutable()).isTrue();

        IpAddress linkLocal = new IpAddress("[fe80::1%eth0]");
        assertThat(linkLocal.value()).doesNotContain("%");
        assertThat(linkLocal.isPubliclyRoutable()).isFalse();
    }

    @Test
    void shouldNormalizeIpv4MappedIpv6Literal() {
        IpAddress address = new IpAddress("::ffff:8.8.8.8");

        assertThat(address.value()).isEqualTo("8.8.8.8");
        assertThat(address.isPubliclyRoutable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "example.com",
                "localhost",
                "999.1.1.1",
                "256.1.1.1",
                "01.2.3.4",
                "1.2.3",
                "1.2.3.4.5",
                "1..2.3",
                "[8.8.8.8]",
                "8.8.8.8%eth0",
                "[2001:db8::1",
                "2001:db8::1]",
                "fe80::1%",
                "fe80::1%eth0%other",
                "2001:4860:4860::8888%eth0",
                "::1%lo",
                "not:ipv6",
                "١.٢.٣.٤",
                "fe80::1%eth0!"
            })
    void shouldRejectHostnamesAndMalformedLiterals(String value) {
        assertThatThrownBy(() -> new IpAddress(value))
                .isInstanceOf(InvalidClientIpException.class);
    }

    @Test
    void shouldRejectMissingAddress() {
        assertThatThrownBy(() -> new IpAddress(null))
                .isInstanceOf(InvalidClientIpException.class);
        assertThatThrownBy(() -> new IpAddress(" "))
                .isInstanceOf(InvalidClientIpException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0.0.0.0",
                "0.1.2.3",
                "10.1.2.3",
                "100.64.1.1",
                "127.0.0.1",
                "169.254.1.1",
                "172.16.0.1",
                "192.0.0.1",
                "192.0.2.1",
                "192.88.99.1",
                "192.168.1.1",
                "198.18.0.1",
                "198.51.100.1",
                "203.0.113.1",
                "224.0.0.1",
                "240.0.0.1",
                "255.255.255.255",
                "::",
                "::1",
                "fe80::1",
                "ff02::1",
                "fc00::1",
                "2001:db8::1"
            })
    void shouldClassifyNonGlobalRangesAsNotPubliclyRoutable(String value) {
        assertThat(new IpAddress(value).isPubliclyRoutable()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1.1.1.1",
                "8.8.8.8",
                "192.0.0.9",
                "192.0.0.10",
                "2001:4860:4860::8888"
            })
    void shouldClassifyGlobalAddressesAsPubliclyRoutable(String value) {
        assertThat(new IpAddress(value).isPubliclyRoutable()).isTrue();
    }
}
