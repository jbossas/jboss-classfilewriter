/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2012 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.classfilewriter.test.annotation;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class FieldAnnotationTest {

    @Test
    public void testIntFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "intField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(10, ((IntValuedAnnotation) field.getDeclaredAnnotations()[0]).value());
    }

    @Test
    public void testClassFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "classField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(String.class, ((ClassValuedAnnotation) field.getDeclaredAnnotations()[0]).value());
    }

    @Test
    public void testEnumFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "enumField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(EnumValuedAnnotation.SimpleEnum.C, ((EnumValuedAnnotation) field.getDeclaredAnnotations()[0]).value());
    }

    /**
     * the members of TimeUnit are actually inner classes, and isEnum returns false
     */
    @Test
    public void testTimeUnitEnumFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "timeUnitField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(TimeUnit.SECONDS, ((TimeUnitValuedAnnotation) field.getDeclaredAnnotations()[0]).value());
    }

    @Test
    public void testAnnotationFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "annotationField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(20, ((AnnotationValuedAnnotation) field.getDeclaredAnnotations()[0]).value().value());
    }

    @Test
    public void testIntArrayFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "intArrayField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(1, ((IntArrayAnnotation) field.getDeclaredAnnotations()[0]).value()[0]);
        Assert.assertEquals(2, ((IntArrayAnnotation) field.getDeclaredAnnotations()[0]).value()[1]);
        Assert.assertEquals(3, ((IntArrayAnnotation) field.getDeclaredAnnotations()[0]).value()[2]);
    }

    @Test
    public void testAnnotationArrayFieldAnnotation() {
        Field field = AnnotationTester.testFieldAnnotations(FieldAnnotationClass.class, "annotationArrayField");
        Assert.assertEquals(1, field.getDeclaredAnnotations().length);
        Assert.assertEquals(10, ((AnnotationArrayValuedAnnotation) field.getDeclaredAnnotations()[0]).value()[0].value());
        Assert.assertEquals(20, ((AnnotationArrayValuedAnnotation) field.getDeclaredAnnotations()[0]).value()[1].value());
    }

}
