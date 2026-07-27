package pl.empik.couponservice.coupon.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyExistsException;
import pl.empik.couponservice.coupon.domain.exception.CouponAlreadyRedeemedException;
import pl.empik.couponservice.coupon.domain.exception.CouponCountryMismatchException;
import pl.empik.couponservice.coupon.domain.exception.CouponExhaustedException;
import pl.empik.couponservice.coupon.domain.exception.CouponNotFoundException;
import pl.empik.couponservice.coupon.domain.exception.CouponServiceException;
import pl.empik.couponservice.coupon.domain.exception.GeoLocationUnavailableException;
import pl.empik.couponservice.coupon.domain.exception.InvalidClientIpException;
import pl.empik.couponservice.coupon.domain.exception.InvalidRequestDataException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(CouponServiceException.class)
    ResponseEntity<ProblemDetail> handleCouponException(
            CouponServiceException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception);
        HttpHeaders headers = new HttpHeaders();

        if (exception instanceof GeoLocationUnavailableException) {
            headers.set(HttpHeaders.RETRY_AFTER, "5");
        }

        return response(
                status,
                titleFor(status),
                exception.getMessage(),
                exception.errorCode(),
                request,
                headers,
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Violation> violations =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
                        .sorted(Comparator.comparing(Violation::field))
                        .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request validation failed.",
                "VALIDATION_ERROR",
                request,
                new HttpHeaders(),
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<Violation> violations =
                exception.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new Violation(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()))
                        .sorted(Comparator.comparing(Violation::field))
                        .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request validation failed.",
                "VALIDATION_ERROR",
                request,
                new HttpHeaders(),
                violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request body is missing or contains malformed JSON.",
                "MALFORMED_JSON",
                request,
                new HttpHeaders(),
                List.of());
    }

    @ExceptionHandler({
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class,
        NoResourceFoundException.class
    })
    ResponseEntity<ProblemDetail> handleFrameworkError(
            Exception exception, HttpServletRequest request) {
        ErrorResponse errorResponse = (ErrorResponse) exception;
        HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
        HttpHeaders headers = frameworkHeaders(exception);

        return response(
                status,
                frameworkTitle(status),
                frameworkDetail(status),
                frameworkErrorCode(status),
                request,
                headers,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled exception while processing {}", request.getRequestURI(), exception);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                "INTERNAL_ERROR",
                request,
                new HttpHeaders(),
                List.of());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request,
            HttpHeaders headers,
            List<Violation> violations) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(problemType(errorCode));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now(clock));

        if (!violations.isEmpty()) {
            problem.setProperty("violations", violations);
        }

        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, headers, status);
    }

    private static HttpStatus statusFor(CouponServiceException exception) {
        if (exception instanceof InvalidRequestDataException
                || exception instanceof InvalidClientIpException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof CouponNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof CouponCountryMismatchException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof CouponAlreadyExistsException
                || exception instanceof CouponAlreadyRedeemedException
                || exception instanceof CouponExhaustedException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof GeoLocationUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String titleFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request";
            case FORBIDDEN -> "Coupon not available in this country";
            case NOT_FOUND -> "Coupon not found";
            case CONFLICT -> "Coupon conflict";
            case SERVICE_UNAVAILABLE -> "Geolocation unavailable";
            default -> "Internal server error";
        };
    }

    private static HttpHeaders frameworkHeaders(Exception exception) {
        HttpHeaders headers = new HttpHeaders();

        if (exception instanceof HttpRequestMethodNotSupportedException methodException
                && methodException.getSupportedHttpMethods() != null) {
            headers.setAllow(methodException.getSupportedHttpMethods());
        }
        if (exception instanceof HttpMediaTypeNotSupportedException mediaTypeException) {
            headers.setAccept(mediaTypeException.getSupportedMediaTypes());
        }

        return headers;
    }

    private static String frameworkTitle(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Resource not found";
            case METHOD_NOT_ALLOWED -> "Method not allowed";
            case NOT_ACCEPTABLE -> "Response media type not acceptable";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type";
            default -> "Request cannot be processed";
        };
    }

    private static String frameworkDetail(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Requested resource does not exist.";
            case METHOD_NOT_ALLOWED -> "HTTP method is not supported for this endpoint.";
            case NOT_ACCEPTABLE -> "Requested response media type is not supported.";
            case UNSUPPORTED_MEDIA_TYPE -> "Request Content-Type is not supported.";
            default -> "Request cannot be processed.";
        };
    }

    private static String frameworkErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case NOT_ACCEPTABLE -> "NOT_ACCEPTABLE";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "REQUEST_REJECTED";
        };
    }

    private static URI problemType(String errorCode) {
        return URI.create(
                "urn:problem:coupon-service:"
                        + errorCode.toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    private record Violation(String field, String message) {}
}
