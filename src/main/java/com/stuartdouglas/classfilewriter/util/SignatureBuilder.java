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
package com.stuartdouglas.classfilewriter.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * Generates the contents of a Signature attribute from a java type.
 * <p>
 * TODO: this is all wrong, I'm to tired to read the pathetic excuse for a spec properly
 * 
 * @author Stuart Douglas
 * 
 */
public class SignatureBuilder {

    /**
     * Generates the field signiture for a field of the given type
     *
     * @param type
     * @return the signiture, or null if no signure is required (e.g. for Class types)
     */
    public static String fieldAttribute(Type type) {
        StringBuilder builder = new StringBuilder();
        fieldAttribute(type, builder);
        return builder.toString();

    }

    private static void fieldAttribute(Type type, StringBuilder builder) {
        if (type instanceof Class<?>) {
            classType((Class<?>) type, builder);
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

    private static void classType(Class<?> clazz, StringBuilder builder) {
        if (clazz.isMemberClass()) {
            classType(clazz.getDeclaringClass(), builder);
            builder.append('.');
            builder.append(clazz.getSimpleName());
        } else {
            builder.append("L");
            builder.append(clazz.getName().replace('.', '/'));
        }

    }

    private SignatureBuilder() {
    }
}
