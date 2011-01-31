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

import java.lang.reflect.Field;

import junit.framework.Assert;

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
        Assert.assertEquals(SimpleEnum.C, ((EnumValuedAnnotation) field.getDeclaredAnnotations()[0]).value());
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
