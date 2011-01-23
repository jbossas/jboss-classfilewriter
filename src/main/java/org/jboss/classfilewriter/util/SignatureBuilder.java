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
package org.jboss.classfilewriter.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

/**
 * Generates the contents of a Signature attribute from a java type.
 *
 * @author Stuart Douglas
 *
 */
public class SignatureBuilder<R, X extends List<R>> {

    public String classSignature(Class<?> clazz) {
        StringBuilder builder = new StringBuilder();
        formalTypeParameters(builder, clazz.getTypeParameters());
        // classTypeSignature(builder, clazz.getGenericSuperclass());
        for (Type iface : clazz.getGenericInterfaces()) {
            // classTypeSignature(builder, iface);
        }
        return builder.toString();
    }

    public static String fieldSignature(Type type) {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        fieldTypeSignature(builder, type);
        builder.append(">");
        return builder.toString();
    }

    private static void classTypeSignature(StringBuilder builder, ParameterizedType genericSuperclass) {
        builder.append("L");
        packageSpecifier(builder, (Class<?>) genericSuperclass.getRawType());
        simpleClassTypeSignatures(builder, genericSuperclass);
        builder.append(';');
    }

    private static void classTypeSignature(StringBuilder builder, Class<?> clazz) {
        builder.append("L");
        packageSpecifier(builder, clazz);
        simpleClassTypeSignatures(builder, clazz);
        builder.append(';');
    }

    private static void simpleClassTypeSignatures(StringBuilder builder, Class<?> clazz) {
        if (clazz.isMemberClass()) {
            simpleClassTypeSignatures(builder, clazz.getDeclaringClass());
        }
        builder.append('.');
        builder.append(clazz.getSimpleName());
        typeArguments(builder, clazz);
    }

    private static void simpleClassTypeSignatures(StringBuilder builder, ParameterizedType type) {
        if (type.getOwnerType() != null) {
            if (type.getOwnerType() instanceof Class<?>) {
                simpleClassTypeSignatures(builder, (Class<?>) type.getOwnerType());
            } else if (type.getOwnerType() instanceof ParameterizedType) {
                simpleClassTypeSignatures(builder, (ParameterizedType) type.getOwnerType());
            }
        }
        builder.append('.');
        builder.append(((Class<?>) type.getRawType()).getSimpleName());
        typeArguments(builder, type);
    }

    private void formalTypeParameters(StringBuilder builder, TypeVariable<?>[] typeParameters) {
        if(typeParameters.length != 0) {
            builder.append('<');
            for(TypeVariable<?> param : typeParameters) {
                formalTypeParameter(builder, param);
            }
            builder.append('>');
        }
    }

    private void formalTypeParameter(StringBuilder builder, TypeVariable<?> param) {
        builder.append(param.getName()); // Identifier
        for (Type bound : param.getBounds()) {
            fieldTypeSignature(builder, bound);
        }
    }

    private static void fieldTypeSignature(StringBuilder builder, Type type) {
        if (type instanceof Class<?>) {
            classTypeSignature(builder, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            classTypeSignature(builder, (ParameterizedType) type);
        } else if (type instanceof GenericArrayType) {
            arrayTypeSignature(builder, type);
        }
    }

    private static void arrayTypeSignature(StringBuilder builder, Type type) {
        // TODO Auto-generated method stub

    }

    private static void typeArguments(StringBuilder builder, ParameterizedType type) {
        fieldAttribute(type.getRawType(), builder); // write the owner type
        // now write the type arguments
        builder.append('<');
        for (Type t : type.getActualTypeArguments()) {
            fieldAttribute(t, builder);
        }
        builder.append(">");
    }

    private static void packageSpecifier(StringBuilder builder, Class<?> clazz) {
        builder.append(clazz.getPackage().getName().replace('.', '/'));
    }

    private static void fieldAttribute(Type type, StringBuilder builder) {
        if (type instanceof Class<?>) {
            classTypeSignature(builder, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            parametizedType(ptype, builder);
        } else if (type instanceof WildcardType) {
            WildcardType ptype = (WildcardType) type;
            wildcardType(ptype, builder);
        }
    }

    private static void wildcardType(WildcardType type, StringBuilder builder) {
        // WRONG
        builder.append('*');
    }

    public static void parametizedType(ParameterizedType type, StringBuilder builder) {
        fieldAttribute(type.getRawType(), builder); //write the owner type
        //now write the type arguments
        builder.append('<');
        for(Type t : type.getActualTypeArguments()) {
            fieldAttribute(t, builder);
            builder.append(';');
        }
        builder.append(">;");
    }

    private SignatureBuilder() {
    }
}
