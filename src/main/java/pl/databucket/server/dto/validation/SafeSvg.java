package pl.databucket.server.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// SVG markup of an icon, checked by SvgCheck
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeSvg.Validator.class)
public @interface SafeSvg {

    String message() default "The icon is not a safe SVG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<SafeSvg, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            String problem = SvgCheck.problem(value);
            if (problem == null)
                return true;
            // the problem may quote the icon (an element name): passed as a parameter, never interpolated itself
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.disableDefaultConstraintViolation();
            hibernateContext.addMessageParameter("problem", problem)
                    .buildConstraintViolationWithTemplate("{problem}")
                    .addConstraintViolation();
            return false;
        }
    }
}
