/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.classfilewriter.test.annotation;

import java.lang.reflect.Method;

import junit.framework.Assert;

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
        Assert.assertEquals(SimpleEnum.C, ((EnumValuedAnnotation) method.getDeclaredAnnotations()[0]).value());
        Assert.assertEquals(1, method.getParameterAnnotations()[0].length);
        Assert.assertEquals(SimpleEnum.C, ((EnumValuedAnnotation) method.getParameterAnnotations()[0][0]).value());
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

}
