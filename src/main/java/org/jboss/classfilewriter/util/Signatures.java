/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.classfilewriter.util;

import static org.jboss.classfilewriter.util.DescriptorUtils.BOOLEAN_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.BYTE_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.CHAR_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.DOUBLE_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.FLOAT_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.INT_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.LONG_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.SHORT_CLASS_DESCRIPTOR;
import static org.jboss.classfilewriter.util.DescriptorUtils.VOID_CLASS_DESCRIPTOR;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Encode signatures that use types outside the type system of the Java Virtual Machine. See also the JVM spec, section "4.7.9.1. Signatures".
 *
 * If anything goes wrong during encoding a {@link RuntimeException} is thrown.
 *
 * @author Martin Kouba
 */
public final class Signatures {

    static final char WILDCARD_UPPER_BOUND = '+';
    static final char WILDCARD_LOWER_BOUND = '-';
    static final char WILDCARD_NO_BOUND = '*';
    static final char TYPE_PARAM_DEL_START = '<';
    static final char TYPE_PARAM_DEL_END = '>';
    static final char SEMICOLON = ';';
    static final char COLON = ':';

    private Signatures() {
    }

    /**
     *
     *
     * @param method
     * @return the JVM method signature
     */
    public static String methodSignature(Method method) {

        StringBuilder builder = new StringBuilder();

        // Type parameters
        TypeVariable<?>[] typeParams = method.getTypeParameters();
        if (typeParams.length > 0) {
            builder.append(TYPE_PARAM_DEL_START);
            for (TypeVariable<?> typeParam : typeParams) {
                typeParameter(typeParam, builder);
            }
            builder.append(TYPE_PARAM_DEL_END);
        }

        // Formal parameters
        Type[] params = method.getGenericParameterTypes();
        builder.append('(');
        if (params.length > 0) {
            for (Type paramType : params) {
                javaType(paramType, builder);
            }
        }
        builder.append(')');

        // Return type
        javaType(method.getGenericReturnType(), builder);

        // Throws
        Type[] exceptions = method.getGenericExceptionTypes();
        if (exceptions.length > 0) {
            // "If the throws clause of a method or constructor declaration does not involve type variables, then a compiler may treat the declaration as having no throws clause for the purpose of emitting a method signature."
            // Note that it's only possible to use a type parameter in a throws clause
            for (Type exceptionType : exceptions) {
                builder.append('^');
                javaType(exceptionType, builder);
            }
        }
        return builder.toString();
    }

    public static String constructorSignature(Constructor method) {

        StringBuilder builder = new StringBuilder();

        // Type parameters
        TypeVariable<?>[] typeParams = method.getTypeParameters();
        if (typeParams.length > 0) {
            builder.append(TYPE_PARAM_DEL_START);
            for (TypeVariable<?> typeParam : typeParams) {
                typeParameter(typeParam, builder);
            }
            builder.append(TYPE_PARAM_DEL_END);
        }

        // Formal parameters
        Type[] params = method.getGenericParameterTypes();
        builder.append('(');
        if (params.length > 0) {
            for (Type paramType : params) {
                javaType(paramType, builder);
            }
        }
        builder.append(')');

        // Return type
        builder.append(VOID_CLASS_DESCRIPTOR);

        // Throws
        Type[] exceptions = method.getGenericExceptionTypes();
        if (exceptions.length > 0) {
            // "If the throws clause of a method or constructor declaration does not involve type variables, then a compiler may treat the declaration as having no throws clause for the purpose of emitting a method signature."
            // Note that it's only possible to use a type parameter in a throws clause
            for (Type exceptionType : exceptions) {
                builder.append('^');
                javaType(exceptionType, builder);
            }
        }
        return builder.toString();
    }

    /**
     * TypeParameter
     *
     * @param typeVariable
     * @param builder
     */
    private static void typeParameter(TypeVariable<?> typeVariable, StringBuilder builder) {
        builder.append(typeVariable.getName());
        Type[] bounds = typeVariable.getBounds();
        if (bounds.length > 0) {
            for (int i = 0; i < bounds.length; i++) {
                // If the first bound is an interface, add additional colon to comply with the spec (ClassBound is not optional)
                if (i == 0 && getTypeParamBoundRawType(bounds[i]).isInterface()) {
                    builder.append(COLON);
                }
                builder.append(COLON);
                javaType(bounds[i], builder);
            }
        } else {
            // If no upper bound is declared, the upper bound is java.lang.Object
            builder.append(COLON);
            javaType(Object.class, builder);
        }
    }

    /**
     * JavaTypeSignature
     *
     * @param type
     * @param builder
     */
    private static void javaType(Type type, StringBuilder builder) {
        if (type instanceof Class) {
            nonGenericType((Class<?>) type, builder);
        } else if (type instanceof ParameterizedType) {
            parameterizedType((ParameterizedType) type, builder);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            builder.append('[');
            javaType(genericArrayType.getGenericComponentType(), builder);
        } else if (type instanceof WildcardType) {
            wildcardType((WildcardType) type, builder);
        } else if (type instanceof TypeVariable) {
            typeVariable((TypeVariable<?>) type, builder);
        } else {
            throw new IllegalArgumentException("Signature encoding error - unsupported type: " + type);
        }
    }

    /**
     * Note that Java language does not support more than one upper/lower bound.
     *
     * @param wildcard
     * @param builder
     */
    private static void wildcardType(WildcardType wildcard, StringBuilder builder) {
        if (wildcard.getLowerBounds().length > 0) {
            for (Type lowerBound : wildcard.getLowerBounds()) {
                builder.append(WILDCARD_LOWER_BOUND);
                javaType(lowerBound, builder);
            }
        } else {
            if (wildcard.getUpperBounds().length == 0 || (wildcard.getUpperBounds().length == 1 && Object.class.equals(wildcard.getUpperBounds()[0]))) {
                // If no upper bound is explicitly declared, the upper bound is java.lang.Object
                // It's not clear whether an empty array may be returned
                builder.append(WILDCARD_NO_BOUND);
            } else {
                for (Type upperBound : wildcard.getUpperBounds()) {
                    builder.append(WILDCARD_UPPER_BOUND);
                    javaType(upperBound, builder);
                }
            }
        }
    }

    private static void typeVariable(TypeVariable<?> typeVariable, StringBuilder builder) {
        builder.append('T');
        builder.append(typeVariable.getName());
        builder.append(SEMICOLON);
    }

    private static void parameterizedType(ParameterizedType parameterizedType, StringBuilder builder) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class) {
            builder.append(classTypeBase(((Class<?>) rawType).getName()));
        } else {
            throw new IllegalStateException(String.format("Signature encoding error - unsupported raw type: %s of parameterized type: %s", parameterizedType,
                    rawType));
        }
        builder.append(TYPE_PARAM_DEL_START);
        for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
            javaType(actualTypeArgument, builder);
        }
        builder.append(TYPE_PARAM_DEL_END);
        builder.append(SEMICOLON);
    }

    /**
     * BaseType, ClassTypeSignature or ArrayTypeSignature
     *
     * @param clazz
     */
    private static void nonGenericType(Class<?> clazz, StringBuilder builder) {
        if (void.class.equals(clazz)) {
            builder.append(VOID_CLASS_DESCRIPTOR);
        } else if (byte.class.equals(clazz)) {
            builder.append(BYTE_CLASS_DESCRIPTOR);
        } else if (char.class.equals(clazz)) {
            builder.append(CHAR_CLASS_DESCRIPTOR);
        } else if (double.class.equals(clazz)) {
            builder.append(DOUBLE_CLASS_DESCRIPTOR);
        } else if (float.class.equals(clazz)) {
            builder.append(FLOAT_CLASS_DESCRIPTOR);
        } else if (int.class.equals(clazz)) {
            builder.append(INT_CLASS_DESCRIPTOR);
        } else if (long.class.equals(clazz)) {
            builder.append(LONG_CLASS_DESCRIPTOR);
        } else if (short.class.equals(clazz)) {
            builder.append(SHORT_CLASS_DESCRIPTOR);
        } else if (boolean.class.equals(clazz)) {
            builder.append(BOOLEAN_CLASS_DESCRIPTOR);
        } else if (clazz.isArray()) {
            builder.append(encodeClassName(clazz.getName()));
        } else {
            builder.append(classTypeBase(clazz.getName()) + SEMICOLON);
        }
    }

    /**
     * ClassTypeSignature base
     *
     * @param clazz
     * @param builder
     */
    private static String classTypeBase(String className) {
        return 'L' + encodeClassName(className);
    }

    private static String encodeClassName(String className) {
        return className.replace('.', '/');
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getTypeParamBoundRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            Type[] bounds = variable.getBounds();
            return getBound(bounds);
        }
        throw new IllegalStateException("Signature encoding error - unexpected type parameter bound type: " + type);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getBound(Type[] bounds) {
        if (bounds.length == 0) {
            return (Class<T>) Object.class;
        } else {
            return getTypeParamBoundRawType(bounds[0]);
        }
    }

}
