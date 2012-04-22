package org.jboss.classfilewriter.test.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Stuart Douglas
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface StringValuedAnnotation {
    String comment();
}
