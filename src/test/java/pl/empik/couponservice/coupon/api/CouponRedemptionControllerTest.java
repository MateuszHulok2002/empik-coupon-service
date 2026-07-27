package pl.empik.couponservice.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.RedeemCouponUseCase;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.GeoLocationUnavailableException;
import pl.empik.couponservice.coupon.domain.value.IpAddress;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");
    private static final IpAddress CLIENT_IP = new IpAddress("8.8.8.8");

    @Mock
    private RedeemCouponUseCase redeemCouponUseCase;

    @Mock
    private ServletClientIpExtractor clientIpExtractor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new CouponRedemptionController(
                                        redeemCouponUseCase, clientIpExtractor))
                        .setControllerAdvice(
                                new ApiExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                        .build();
    }

    @Test
    void shouldRedeemCouponUsingIpTakenFromServletRequest() throws Exception {
        UUID redemptionId = UUID.randomUUID();
        when(clientIpExtractor.extract(any(HttpServletRequest.class))).thenReturn(CLIENT_IP);
        when(redeemCouponUseCase.redeem(any()))
                .thenReturn(
                        new RedemptionResult(
                                redemptionId, "WIOSNA", "user-1", NOW, 1, 9, "PL"));

        mockMvc.perform(
                        post("/api/v1/coupon-redemptions")
                                .with(request -> {
                                    request.setRemoteAddr("8.8.8.8");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "couponCode": "wiosna",
                                          "userId": "user-1"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.redemptionId").value(redemptionId.toString()))
                .andExpect(jsonPath("$.couponCode").value("WIOSNA"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.currentUses").value(1))
                .andExpect(jsonPath("$.remainingUses").value(9))
                .andExpect(jsonPath("$.country").value("PL"));

        ArgumentCaptor<RedeemCouponCommand> commandCaptor =
                ArgumentCaptor.forClass(RedeemCouponCommand.class);
        verify(redeemCouponUseCase).redeem(commandCaptor.capture());

        assertThat(commandCaptor.getValue().couponCode().value()).isEqualTo("WIOSNA");
        assertThat(commandCaptor.getValue().userId().value()).isEqualTo("user-1");
        assertThat(commandCaptor.getValue().clientIp()).isEqualTo(CLIENT_IP);
    }

    @Test
    void shouldReturnForbiddenForWrongCountry() throws Exception {
        when(clientIpExtractor.extract(any(HttpServletRequest.class))).thenReturn(CLIENT_IP);
        when(redeemCouponUseCase.redeem(any()))
                .thenThrow(new CouponCountryMismatchException("WIOSNA", "PL", "DE"));

        mockMvc.perform(
                        post("/api/v1/coupon-redemptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"couponCode":"WIOSNA","userId":"user-1"}
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COUPON_COUNTRY_MISMATCH"));
    }

    @Test
    void shouldReturnServiceUnavailableAndRetryAfterWhenGeoProviderFails() throws Exception {
        when(clientIpExtractor.extract(any(HttpServletRequest.class))).thenReturn(CLIENT_IP);
        when(redeemCouponUseCase.redeem(any()))
                .thenThrow(new GeoLocationUnavailableException("Temporary failure."));

        mockMvc.perform(
                        post("/api/v1/coupon-redemptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"couponCode":"WIOSNA","userId":"user-1"}
                                        """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "5"))
                .andExpect(jsonPath("$.errorCode").value("GEOLOCATION_UNAVAILABLE"));
    }
}
