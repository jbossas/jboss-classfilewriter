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
package org.jboss.classfilewriter.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * Utility class that can be used to contruct annotations and annotation attributes from java {@link Annotation} instances
 *
 * @author Stuart Douglas
 */
public class AnnotationBuilder {

    public static ClassAnnotation createAnnotation(ConstPool constPool, final Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        List<AnnotationValue> values = new ArrayList<AnnotationValue>();
        try {
            for (final Method m : annotationType.getDeclaredMethods()) {
                Object value = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws InvocationTargetException, IllegalAccessException {
                        m.setAccessible(true);
                        return m.invoke(annotation);
                    }
                });

                values.add(createValue(constPool, m.getName(), value));
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (PrivilegedActionException e) {
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
        } else if (type.isEnum() || (type.getEnclosingClass() != null && type.getEnclosingClass().isEnum())) {
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
