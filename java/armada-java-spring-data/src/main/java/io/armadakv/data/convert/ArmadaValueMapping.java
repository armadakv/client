/*
 * Copyright JAMF Software, LLC
 */

package io.armadakv.data.convert;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ArmadaValueMapping {
    Type value() default Type.JSON;

    enum Type {
        XML, JSON
    }
}
