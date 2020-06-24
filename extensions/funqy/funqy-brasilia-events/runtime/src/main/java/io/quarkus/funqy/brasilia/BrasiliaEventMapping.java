package io.quarkus.funqy.brasilia;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrasiliaEventMapping {

    /**
     * @return
     */
    String name() default "";

    /**
     * @return
     */
    String version() default "";

    /**
     * @return
     */
    String configJSON() default "";
}
