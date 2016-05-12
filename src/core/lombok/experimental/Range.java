package lombok.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If placed on a parameter, lombok will insert a check at the start of the method for the parameter to be in the range
 * given to the annotation. For example, putting {@code @Range({0, 42}} on a parameter will generate an
 * {@code IllegalArgumentException} that will be thrown if the value is less than zero or greater than 42.
 * <p>
 * This annotation must be passed exactly <i>two</i> values -- any more or less will result in a compilation error.
 * These values must also be <i>literals</i>. Using field references (e.g {@code @Range({SOME_MIN, SOME_MAX}}) will fail
 * to compile.
 * </p>
 * <p>
 * This is shorthand for {@link Min @Min(min_value)} {@link Max @Max(max_value)}. This takes precedence over {@link Min},
 * {@link Max}, and {@link NonNegative}; if any of those annotations are placed on the same element as this one,
 * only the checks for {@code @Range} will be generated and the compiler will issue a warning.
 * </p>
 *
 * @see Min
 * @see Max
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Range {
    double[] value();
}
