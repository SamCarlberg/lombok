package lombok.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If placed on a parameter, lombok will insert a check at the start of the method for the parameter to be less than
 * or equal to the value in the annotation. For example, placing {@code @Max(4.2)} before a parameter will throw an
 * {@code IllegalArgumentException} if the value of that parameter is greater than 4.2.
 * <p>
 * Note that the argument must be a <i>literal</i> (e.g. {@code @Max(3.14)}). Passing a field or constant (e.g.
 * {@code @Max(Math.PI)}) will fail to compile.
 * </p>
 *
 * @see Min
 * @see Range
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Max {
    double value();
}
