package pl.empik.couponservice.coupon.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.CreateCouponUseCase;
import pl.empik.couponservice.coupon.application.result.CouponResult;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");

    @Mock
    private CreateCouponUseCase createCouponUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Jackson2ObjectMapperBuilder registers Spring's ProblemDetailJacksonMixin, so RFC 9457
        // extension fields (errorCode, timestamp, violations) stay top-level exactly as in the
        // real application; a raw ObjectMapper would nest them under "properties".
        ObjectMapper objectMapper =
                Jackson2ObjectMapperBuilder.json()
                        .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();

        mockMvc =
                MockMvcBuilders.standaloneSetup(new CouponController(createCouponUseCase))
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .setControllerAdvice(
                                new ApiExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                        .build();
    }

    @Test
    void shouldCreateCoupon() throws Exception {
        UUID id = UUID.randomUUID();
        when(createCouponUseCase.create(any()))
                .thenReturn(new CouponResult(id, "WIOSNA", NOW, 10, 0, 10, "PL"));

        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "code": "wiosna",
                                          "maxUses": 10,
                                          "country": "pl"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.maxUses").value(10))
                .andExpect(jsonPath("$.currentUses").value(0))
                .andExpect(jsonPath("$.remainingUses").value(10))
                .andExpect(jsonPath("$.country").value("PL"));

        ArgumentCaptor<CreateCouponCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateCouponCommand.class);
        verify(createCouponUseCase).create(commandCaptor.capture());

        assertThat(commandCaptor.getValue().code().value()).isEqualTo("WIOSNA");
        assertThat(commandCaptor.getValue().country().value()).isEqualTo("PL");
    }

    @Test
    void shouldReturnProblemDetailsForValidationError() throws Exception {
        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "code": "",
                                          "maxUses": 0,
                                          "country": "P"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").value(NOW.toString()))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void shouldReturnConflictWhenCodeAlreadyExists() throws Exception {
        when(createCouponUseCase.create(any()))
                .thenThrow(new CouponAlreadyExistsException("WIOSNA"));

        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "code": "WIOSNA",
                                          "maxUses": 10,
                                          "country": "PL"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("WIOSNA")));
    }

    @Test
    void shouldRejectUnknownJsonProperty() throws Exception {
        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "code": "WIOSNA",
                                          "maxUses": 10,
                                          "country": "PL",
                                          "unexpected": true
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }

    @Test
    void shouldPreserveMethodNotAllowedSemantics() throws Exception {
        mockMvc.perform(get("/api/v1/coupons"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void shouldPreserveUnsupportedMediaTypeSemantics() throws Exception {
        mockMvc.perform(
                        post("/api/v1/coupons")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
    }
}
