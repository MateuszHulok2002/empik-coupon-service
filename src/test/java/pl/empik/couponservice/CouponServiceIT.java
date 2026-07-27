package pl.empik.couponservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pl.empik.couponservice.coupon.application.command.CreateCouponCommand;
import pl.empik.couponservice.coupon.application.command.RedeemCouponCommand;
import pl.empik.couponservice.coupon.application.port.in.CreateCouponUseCase;
import pl.empik.couponservice.coupon.application.port.in.RedeemCouponUseCase;
import pl.empik.couponservice.coupon.application.result.RedemptionResult;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyExistsException;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponExhaustedException;
import pl.empik.couponservice.coupon.domain.value.CountryCode;
import pl.empik.couponservice.coupon.domain.value.CouponCode;
import pl.empik.couponservice.coupon.domain.value.IpAddress;
import pl.empik.couponservice.coupon.domain.value.UserId;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.geo.mode=fixed",
            "app.geo.fixed-country=PL",
            "spring.datasource.hikari.connection-timeout=30000",
            "spring.jpa.properties.hibernate.generate_statistics=false"
        })
class CouponServiceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.10-alpine"))
                    .withDatabaseName("coupons")
                    .withUsername("coupon")
                    .withPassword("coupon");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CreateCouponUseCase createCouponUseCase;

    @Autowired
    private RedeemCouponUseCase redeemCouponUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM coupon_redemptions");
        jdbcTemplate.update("DELETE FROM coupons");
    }

    @Test
    void shouldEnforceCompleteCouponLifecycleAndCaseInsensitiveUniqueness() {
        createCouponUseCase.create(
                new CreateCouponCommand(
                        new CouponCode("wiosna"), 2, new CountryCode("PL")));

        assertThatThrownBy(
                        () ->
                                createCouponUseCase.create(
                                        new CreateCouponCommand(
                                                new CouponCode("WIOSNA"),
                                                2,
                                                new CountryCode("PL"))))
                .isInstanceOf(CouponAlreadyExistsException.class);

        RedemptionResult first =
                redeem("Wiosna", "user-1");
        RedemptionResult second =
                redeem("WIOSNA", "user-2");

        assertThat(first.currentUses()).isEqualTo(1);
        assertThat(second.currentUses()).isEqualTo(2);
        assertThat(second.remainingUses()).isZero();

        assertThatThrownBy(() -> redeem("WIOSNA", "user-1"))
                .isInstanceOf(CouponAlreadyRedeemedException.class);

        assertThatThrownBy(() -> redeem("WIOSNA", "user-3"))
                .isInstanceOf(CouponExhaustedException.class);

        assertThat(currentUses("WIOSNA")).isEqualTo(2);
        assertThat(redemptionCount("WIOSNA")).isEqualTo(2);
    }

    @Test
    void shouldExposeProblemDetailExtensionsAtTopLevelOverRealHttp() {
        String requestBody =
                """
                {
                  "code": "http-contract",
                  "maxUses": 1,
                  "country": "PL"
                }
                """;

        ResponseEntity<JsonNode> created =
                restTemplate.postForEntity(
                        "/api/v1/coupons", jsonRequest(requestBody), JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(created.getBody()).path("code").asText())
                .isEqualTo("HTTP-CONTRACT");

        ResponseEntity<JsonNode> duplicate =
                restTemplate.postForEntity(
                        "/api/v1/coupons", jsonRequest(requestBody), JsonNode.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        MediaType contentType = duplicate.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();

        JsonNode problem = Objects.requireNonNull(duplicate.getBody());
        assertThat(problem.path("errorCode").asText()).isEqualTo("COUPON_ALREADY_EXISTS");
        assertThat(problem.path("type").asText())
                .isEqualTo("urn:problem:coupon-service:coupon-already-exists");
        assertThat(problem.path("instance").asText()).isEqualTo("/api/v1/coupons");
        assertThat(problem.path("timestamp").isTextual()).isTrue();
        assertThatCode(() -> Instant.parse(problem.path("timestamp").asText()))
                .doesNotThrowAnyException();
        assertThat(problem.has("properties")).isFalse();
    }

    @Test
    void shouldProtectCoreInvariantsAtDatabaseLevel() {
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO coupons (
                                            id, code, created_at, max_uses, current_uses, country_code
                                        ) VALUES (?, ?, now(), 1, 0, 'PL')
                                        """,
                                        UUID.randomUUID(),
                                        "lowercase"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO coupons (
                                            id, code, created_at, max_uses, current_uses, country_code
                                        ) VALUES (?, ?, now(), 1, 0, 'PL')
                                        """,
                                        UUID.randomUUID(),
                                        "BAD\nCODE"))
                .isInstanceOf(DataIntegrityViolationException.class);

        createCouponUseCase.create(
                new CreateCouponCommand(
                        new CouponCode("LIMITED"), 1, new CountryCode("PL")));

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "UPDATE coupons SET current_uses = 2 WHERE code = 'LIMITED'"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(currentUses("LIMITED")).isZero();

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        """
                                        INSERT INTO coupon_redemptions (
                                            id, coupon_id, user_id, redeemed_at, resolved_country_code
                                        ) VALUES (?, ?, ?, now(), 'PL')
                                        """,
                                        UUID.randomUUID(),
                                        couponId("LIMITED"),
                                        "bad\tuser"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectCouponFromCountryOtherThanResolvedClientCountry() {
        createCouponUseCase.create(
                new CreateCouponCommand(
                        new CouponCode("DE_ONLY"), 10, new CountryCode("DE")));

        assertThatThrownBy(() -> redeem("DE_ONLY", "user-1"))
                .isInstanceOf(CouponCountryMismatchException.class);

        assertThat(currentUses("DE_ONLY")).isZero();
        assertThat(redemptionCount("DE_ONLY")).isZero();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldCreateOnlyOneCouponForConcurrentCaseVariants() throws Exception {
        int attempts = 20;

        List<AttemptOutcome> outcomes =
                runConcurrently(
                        attempts,
                        index ->
                                createCouponUseCase.create(
                                        new CreateCouponCommand(
                                                new CouponCode(index % 2 == 0 ? "flash-sale" : "FLASH-SALE"),
                                                10,
                                                new CountryCode("PL"))));

        assertThat(outcomes.stream().filter(AttemptOutcome::success).count()).isEqualTo(1);
        assertThat(
                        outcomes.stream()
                                .filter(
                                        outcome ->
                                                outcome.failure()
                                                        instanceof CouponAlreadyExistsException)
                                .count())
                .isEqualTo(attempts - 1);
        assertThat(couponCount("FLASH-SALE")).isEqualTo(1);
        assertThat(currentUses("FLASH-SALE")).isZero();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldNeverExceedUsageLimitUnderConcurrentLoad() throws Exception {
        int limit = 10;
        int attempts = 50;

        createCouponUseCase.create(
                new CreateCouponCommand(
                        new CouponCode("FLASH"), limit, new CountryCode("PL")));

        List<AttemptOutcome> outcomes =
                runConcurrently(
                        attempts,
                        index ->
                                redeemCouponUseCase.redeem(
                                        new RedeemCouponCommand(
                                                new CouponCode("flash"),
                                                new UserId("user-" + index),
                                                new IpAddress("127.0.0.1"))));

        assertThat(outcomes.stream().filter(AttemptOutcome::success).count())
                .isEqualTo(limit);
        assertThat(
                        outcomes.stream()
                                .filter(outcome -> outcome.failure() instanceof CouponExhaustedException)
                                .count())
                .isEqualTo(attempts - limit);
        assertThat(currentUses("FLASH")).isEqualTo(limit);
        assertThat(redemptionCount("FLASH")).isEqualTo(limit);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldAllowOnlyOneConcurrentRedemptionPerUser() throws Exception {
        int attempts = 20;

        createCouponUseCase.create(
                new CreateCouponCommand(
                        new CouponCode("ONCE"), attempts, new CountryCode("PL")));

        List<AttemptOutcome> outcomes =
                runConcurrently(
                        attempts,
                        ignored ->
                                redeemCouponUseCase.redeem(
                                        new RedeemCouponCommand(
                                                new CouponCode("ONCE"),
                                                new UserId("same-user"),
                                                new IpAddress("127.0.0.1"))));

        assertThat(outcomes.stream().filter(AttemptOutcome::success).count()).isEqualTo(1);
        assertThat(
                        outcomes.stream()
                                .filter(
                                        outcome ->
                                                outcome.failure()
                                                        instanceof CouponAlreadyRedeemedException)
                                .count())
                .isEqualTo(attempts - 1);
        assertThat(currentUses("ONCE")).isEqualTo(1);
        assertThat(redemptionCount("ONCE")).isEqualTo(1);
    }

    private static HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private RedemptionResult redeem(String code, String userId) {
        return redeemCouponUseCase.redeem(
                new RedeemCouponCommand(
                        new CouponCode(code),
                        new UserId(userId),
                        new IpAddress("127.0.0.1")));
    }

    private UUID couponId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM coupons WHERE code = ?", UUID.class, code);
    }

    private long couponCount(String code) {
        Long value =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM coupons WHERE code = ?", Long.class, code);
        return value == null ? 0 : value;
    }

    private int currentUses(String code) {
        Integer value =
                jdbcTemplate.queryForObject(
                        "SELECT current_uses FROM coupons WHERE code = ?",
                        Integer.class,
                        code);
        return value == null ? 0 : value;
    }

    private long redemptionCount(String code) {
        Long value =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM coupon_redemptions r
                        JOIN coupons c ON c.id = r.coupon_id
                        WHERE c.code = ?
                        """,
                        Long.class,
                        code);
        return value == null ? 0 : value;
    }

    private static List<AttemptOutcome> runConcurrently(
            int attempts, ThrowingIntFunction action) throws Exception {
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AttemptOutcome>> futures = new ArrayList<>(attempts);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (int index = 0; index < attempts; index++) {
                int attempt = index;
                futures.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    start.await();
                                    try {
                                        action.apply(attempt);
                                        return AttemptOutcome.succeeded();
                                    } catch (RuntimeException exception) {
                                        return AttemptOutcome.failed(exception);
                                    }
                                }));
            }

            boolean allWorkersReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(allWorkersReady).isTrue();

            List<AttemptOutcome> outcomes = new ArrayList<>(attempts);
            for (Future<AttemptOutcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            // Never leave workers blocked on the start gate when setup or an assertion fails.
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingIntFunction {
        void apply(int index);
    }

    private record AttemptOutcome(boolean success, RuntimeException failure) {

        static AttemptOutcome succeeded() {
            return new AttemptOutcome(true, null);
        }

        static AttemptOutcome failed(RuntimeException failure) {
            return new AttemptOutcome(false, failure);
        }
    }
}
