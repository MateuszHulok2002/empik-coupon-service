package pl.empik.couponservice.coupon.infrastructure.persistence;

import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

final class DatabaseConstraintInspector {

    private DatabaseConstraintInspector() {}

    static boolean containsConstraint(Throwable throwable, String constraintName) {
        String expected = constraintName.toLowerCase(Locale.ROOT);
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().equalsIgnoreCase(constraintName)) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }
}
