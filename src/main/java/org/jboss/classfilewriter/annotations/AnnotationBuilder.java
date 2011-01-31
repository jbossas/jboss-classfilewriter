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
package org.jboss.classfilewriter.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * Utility class that can be used to contruct annotations and annotation attributes from java {@link Annotation} instances
 *
 * @author Stuart Douglas
 *
 */
public class AnnotationBuilder {

    public static ClassAnnotation createAnnotation(ConstPool constPool, Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        List<AnnotationValue> values = new ArrayList<AnnotationValue>();
        try {
            for (Method m : annotationType.getDeclaredMethods()) {
                Object value = m.invoke(annotation);
                values.add(createValue(constPool, m.getName(), value));
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return new ClassAnnotation(constPool, annotationType.getName(), values);
    }

    public static AnnotationValue createValue(ConstPool constPool, String name, Object value) {
        Class<?> type = value.getClass();
        if (type == String.class) {
            return new StringAnnotationValue(constPool, name, (String) value);
        } else if (type == int.class || type == Integer.class) {
            return new IntAnnotationValue(constPool, name, (Integer) value);
        } else if (type == short.class || type == Short.class) {
            return new ShortAnnotationValue(constPool, name, (Short) value);
        } else if (type == byte.class || type == Byte.class) {
            return new ByteAnnotationValue(constPool, name, (Byte) value);
        } else if (type == boolean.class || type == Boolean.class) {
            return new BooleanAnnotationValue(constPool, name, (Boolean) value);
        } else if (type == char.class || type == Character.class) {
            return new CharAnnotationValue(constPool, name, (Character) value);
        } else if (type == long.class || type == Long.class) {
            return new LongAnnotationValue(constPool, name, (Long) value);
        } else if (type == float.class || type == Float.class) {
            return new FloatAnnotationValue(constPool, name, (Float) value);
        } else if (type == double.class || type == Double.class) {
            return new DoubleAnnotationValue(constPool, name, (Double) value);
        } else if (type == Class.class) {
            return new ClassAnnotationValue(constPool, name, (Class<?>) value);
        } else if (type.isEnum()) {
            return new EnumAnnotationValue(constPool, name, (Enum<?>) value);
        } else if (value instanceof Annotation) {
            return new AnnotationAnnotationValue(constPool, name, createAnnotation(constPool, (Annotation) value));
        } else if (type.isArray()) {
            int length = Array.getLength(value);
            List<AnnotationValue> values = new ArrayList<AnnotationValue>();
            for (int i = 0; i < length; ++i) {
                values.add(createValue(constPool, null, Array.get(value, i)));
            }
            return new ArrayAnnotationValue(constPool, name, values);
        }
        throw new RuntimeException("Invalid type for annotation value. Type: " + type + " Value: " + value);
    }

    private AnnotationBuilder() {

    }

}
