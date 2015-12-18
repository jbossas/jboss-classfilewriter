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
package org.jboss.classfilewriter.util;

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
            builder.append('L');
            builder.append(clazz.getName().replace('.', '/'));
        }

    }

    private SignatureBuilder() {
    }
}
