package pl.empik.couponservice.coupon.domain.value;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import pl.empik.couponservice.coupon.domain.exception.InvalidClientIpException;

public record IpAddress(String value) {

    private static final Pattern IPV4_LITERAL =
            Pattern.compile(
                    "(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])"
                            + "(?:\\.(?:25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}");
    private static final Pattern ZONE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_.-]+");

    public IpAddress {
        value = normalize(value);
    }

    public boolean isPubliclyRoutable() {
        InetAddress address = parseLiteral(value);

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        if (address instanceof Inet4Address ipv4) {
            return isPublicIpv4(ipv4.getAddress());
        }

        if (address instanceof Inet6Address ipv6) {
            return isPublicIpv6(ipv6.getAddress());
        }

        return false;
    }

    private static boolean isPublicIpv4(byte[] octets) {
        int first = Byte.toUnsignedInt(octets[0]);
        int second = Byte.toUnsignedInt(octets[1]);
        int third = Byte.toUnsignedInt(octets[2]);
        int fourth = Byte.toUnsignedInt(octets[3]);

        if (first == 0) {
            return false;
        }

        // Carrier-grade NAT 100.64.0.0/10.
        if (first == 100 && second >= 64 && second <= 127) {
            return false;
        }

        // IETF protocol assignments 192.0.0.0/24. The two anycast addresses
        // 192.0.0.9 and 192.0.0.10 are globally reachable exceptions.
        if (first == 192
                && second == 0
                && third == 0
                && fourth != 9
                && fourth != 10) {
            return false;
        }

        // Documentation, benchmarking, deprecated relay and reserved ranges.
        return !((first == 192 && second == 0 && third == 2)
                || (first == 192 && second == 88 && third == 99)
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113)
                || (first == 198 && (second == 18 || second == 19))
                || first >= 240);
    }

    private static boolean isPublicIpv6(byte[] bytes) {
        // Unique local addresses fc00::/7.
        if ((bytes[0] & 0xfe) == 0xfc) {
            return false;
        }

        // Documentation range 2001:db8::/32.
        return !(Byte.toUnsignedInt(bytes[0]) == 0x20
                && Byte.toUnsignedInt(bytes[1]) == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0d
                && Byte.toUnsignedInt(bytes[3]) == 0xb8);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientIpException("Client IP address is missing.");
        }

        String candidate = rawValue.trim();
        boolean startsWithBracket = candidate.startsWith("[");
        boolean endsWithBracket = candidate.endsWith("]");

        if (startsWithBracket != endsWithBracket) {
            throw invalidLiteral();
        }
        if (startsWithBracket) {
            candidate = candidate.substring(1, candidate.length() - 1);
            if (!candidate.contains(":")) {
                throw new InvalidClientIpException(
                        "Brackets are only valid around an IPv6 address.");
            }
        }

        boolean hasZoneIdentifier = candidate.indexOf('%') >= 0;
        candidate = removeIpv6Zone(candidate);

        boolean looksLikeIpv4 = IPV4_LITERAL.matcher(candidate).matches();
        boolean looksLikeIpv6 = candidate.contains(":");
        if (!looksLikeIpv4 && !looksLikeIpv6) {
            throw invalidLiteral();
        }

        InetAddress parsed = parseLiteral(candidate);
        if (looksLikeIpv4 && !(parsed instanceof Inet4Address)) {
            throw new InvalidClientIpException("Client IPv4 address is invalid.");
        }
        if (hasZoneIdentifier && !parsed.isLinkLocalAddress()) {
            throw new InvalidClientIpException(
                    "An IPv6 zone identifier is only valid for a link-local address.");
        }

        return parsed.getHostAddress();
    }

    private static String removeIpv6Zone(String candidate) {
        int zoneIndex = candidate.indexOf('%');
        if (zoneIndex < 0) {
            return candidate;
        }

        String zone = candidate.substring(zoneIndex + 1);
        boolean hasSecondSeparator = candidate.indexOf('%', zoneIndex + 1) >= 0;
        if (!candidate.contains(":")
                || zoneIndex == 0
                || hasSecondSeparator
                || !ZONE_IDENTIFIER.matcher(zone).matches()) {
            throw new InvalidClientIpException("Client IPv6 zone identifier is invalid.");
        }

        return candidate.substring(0, zoneIndex);
    }

    private static InetAddress parseLiteral(String candidate) {
        try {
            // Host names are rejected before this method. IPv4 values match a strict decimal
            // pattern and IPv6 values contain ':', so the JDK validates a numeric literal and
            // cannot fall back to DNS host-name resolution.
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException exception) {
            throw new InvalidClientIpException("Client IP address is invalid.");
        }
    }

    private static InvalidClientIpException invalidLiteral() {
        return new InvalidClientIpException(
                "Client IP address must be a numeric IPv4 or IPv6 literal.");
    }

    @Override
    public String toString() {
        return value;
    }
}
