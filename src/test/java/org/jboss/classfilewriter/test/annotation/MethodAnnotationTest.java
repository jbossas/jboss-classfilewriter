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

import java.lang.reflect.Method;

import org.junit.Assert;

import org.junit.Test;

public class MethodAnnotationTest {

    @Test
    public void testIntMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "intMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(10, ((IntValuedAnnotation) method.getDeclaredAnnotations()[0]).value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(10, ((IntValuedAnnotation) method.getParameterAnnotations()[0][0]).value());
    }
    @Test
    public void testStringMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "stringMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals("string", ((StringValuedAnnotation) method.getDeclaredAnnotations()[0]).comment());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals("string", ((StringValuedAnnotation) method.getParameterAnnotations()[0][0]).comment());
    }

    @Test
    public void testClassMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "classMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(void.class, ((ClassValuedAnnotation) method.getDeclaredAnnotations()[0]).value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(void.class, ((ClassValuedAnnotation) method.getParameterAnnotations()[0][0]).value());
    }

    @Test
    public void testEnumMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "enumMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(EnumValuedAnnotation.SimpleEnum.C, ((EnumValuedAnnotation) method.getDeclaredAnnotations()[0]).value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(EnumValuedAnnotation.SimpleEnum.C, ((EnumValuedAnnotation) method.getParameterAnnotations()[0][0]).value());
    }

    @Test
    public void testAnnotationMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "annotationMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(20, ((AnnotationValuedAnnotation) method.getDeclaredAnnotations()[0]).value().value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(20, ((AnnotationValuedAnnotation) method.getParameterAnnotations()[0][0]).value().value());
    }

    @Test
    public void testIntArrayMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "intArrayMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(1, ((IntArrayAnnotation) method.getDeclaredAnnotations()[0]).value()[0]);
        Assert.assertEquals(2, ((IntArrayAnnotation) method.getDeclaredAnnotations()[0]).value()[1]);
        Assert.assertEquals(3, ((IntArrayAnnotation) method.getDeclaredAnnotations()[0]).value()[2]);
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(1, ((IntArrayAnnotation) method.getParameterAnnotations()[0][0]).value()[0]);
        Assert.assertEquals(2, ((IntArrayAnnotation) method.getParameterAnnotations()[0][0]).value()[1]);
        Assert.assertEquals(3, ((IntArrayAnnotation) method.getParameterAnnotations()[0][0]).value()[2]);
    }

    @Test
    public void testAnnotationArrayMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "annotationArrayMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(10, ((AnnotationArrayValuedAnnotation) method.getDeclaredAnnotations()[0]).value()[0].value());
        Assert.assertEquals(20, ((AnnotationArrayValuedAnnotation) method.getDeclaredAnnotations()[0]).value()[1].value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(10, ((AnnotationArrayValuedAnnotation) method.getParameterAnnotations()[0][0]).value()[0].value());
        Assert.assertEquals(20, ((AnnotationArrayValuedAnnotation) method.getParameterAnnotations()[0][0]).value()[1].value());
    }

    @Test
    public void testClassArrayMethodAnnotation() {
        Method method = AnnotationTester.testMethodAnnotations(MethodAnnotationClass.class, "classArrayMethod");
        Assert.assertEquals(1, method.getDeclaredAnnotations().length);
        Assert.assertEquals(String.class, ((ClassArrayValuedAnnotation) method.getDeclaredAnnotations()[0]).value()[0]);
        Assert.assertEquals(int.class, ((ClassArrayValuedAnnotation) method.getDeclaredAnnotations()[0]).value()[1]);
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(String.class, ((ClassArrayValuedAnnotation) method.getParameterAnnotations()[0][0]).value()[0]);
        Assert.assertEquals(int.class, ((ClassArrayValuedAnnotation) method.getParameterAnnotations()[0][0]).value()[1]);
    }
}
