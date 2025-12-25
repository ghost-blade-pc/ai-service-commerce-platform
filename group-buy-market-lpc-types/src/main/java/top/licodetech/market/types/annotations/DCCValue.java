package top.licodetech.market.types.annotations;

import java.lang.annotation.*;

/**
 * @author LiPC
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface DCCValue {

    String value() default "";

}
