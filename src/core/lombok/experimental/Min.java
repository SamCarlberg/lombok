package lombok.experimental;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If placed on a parameter, lombok will insert a check at the start of the method for the parameter to be greater than
 * or equal to the value in the annotation. For example, placing {@code @Min(0.5)} before a parameter will throw an
 * {@code IllegalArgumentException} if the value of that parameter is less than 0.5.
 * <p>
 * Note that the argument must be a <i>literal</i> (e.g. {@code @Min(3.14)}). Passing a field or constant (e.g.
 * {@code @Min(Math.PI)}) will fail to compile.
 * </p>
 *
 * @see Max
 * @see Range
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Min {
    double value();
}
