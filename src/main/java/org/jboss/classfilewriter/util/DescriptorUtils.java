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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for working with method descriptors
 *
 * @author Stuart Douglas
 */
public class DescriptorUtils {

    static final String VOID_CLASS_DESCRIPTOR = "V";
    static final String BYTE_CLASS_DESCRIPTOR = "B";
    static final String CHAR_CLASS_DESCRIPTOR = "C";
    static final String DOUBLE_CLASS_DESCRIPTOR = "D";
    static final String FLOAT_CLASS_DESCRIPTOR = "F";
    static final String INT_CLASS_DESCRIPTOR = "I";
    static final String LONG_CLASS_DESCRIPTOR = "J";
    static final String SHORT_CLASS_DESCRIPTOR = "S";
    static final String BOOLEAN_CLASS_DESCRIPTOR = "Z";

    /**
     * Changes a class name to the internal form suitable for use in a descriptor string.
     * <p/>
     * e.g. java.lang.String => Ljava/lang/String;
     */
    public static String makeDescriptor(String className) {
        String repl = className.replace('.', '/');
        return 'L' + repl + ';';
    }

    public static String makeDescriptor(Class<?> c) {
        if (void.class.equals(c)) {
            return VOID_CLASS_DESCRIPTOR;
        } else if (byte.class.equals(c)) {
            return BYTE_CLASS_DESCRIPTOR;
        } else if (char.class.equals(c)) {
            return CHAR_CLASS_DESCRIPTOR;
        } else if (double.class.equals(c)) {
            return DOUBLE_CLASS_DESCRIPTOR;
        } else if (float.class.equals(c)) {
            return FLOAT_CLASS_DESCRIPTOR;
        } else if (int.class.equals(c)) {
            return INT_CLASS_DESCRIPTOR;
        } else if (long.class.equals(c)) {
            return LONG_CLASS_DESCRIPTOR;
        } else if (short.class.equals(c)) {
            return SHORT_CLASS_DESCRIPTOR;
        } else if (boolean.class.equals(c)) {
            return BOOLEAN_CLASS_DESCRIPTOR;
        } else if (c.isArray()) {
            return c.getName().replace('.', '/');
        } else
        // normal object
        {
            return makeDescriptor(c.getName());
        }
    }

    public static String makeDescriptor(Constructor<?> c) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> p : c.getParameterTypes()) {
            desc.append(DescriptorUtils.makeDescriptor(p));
        }
        desc.append(")");
        desc.append("V");
        return desc.toString();
    }

    /**
     * returns an array of String representations of the parameter types. Primitives are returned as their native
     * representations, while clases are returned in the internal descriptor form e.g. Ljava/lang/Integer;
     */
    public static String[] parameterDescriptors(String methodDescriptor) {
        int i = 1; // char 0 is a '('
        List<String> ret = new ArrayList<String>();
        int arraystart = -1;
        while (methodDescriptor.charAt(i) != ')') {
            String type = null;
            if (methodDescriptor.charAt(i) == '[') {
                if (arraystart == -1) {
                    arraystart = i;
                }
            } else {
                if (methodDescriptor.charAt(i) == 'L') {
                    int start = i;
                    i++;
                    while (methodDescriptor.charAt(i) != ';') {
                        ++i;
                    }
                    if (arraystart == -1) {
                        type = methodDescriptor.substring(start, i + 1);
                    } else {
                        type = methodDescriptor.substring(arraystart, i + 1);
                    }
                } else {
                    if (arraystart == -1) {
                        type = methodDescriptor.charAt(i) + "";
                    } else {
                        type = methodDescriptor.substring(arraystart, i + 1);
                    }
                }
                arraystart = -1;
                ret.add(type);
            }
            ++i;
        }
        String[] r = new String[ret.size()];
        for (int j = 0; j < ret.size(); ++j) {
            r[j] = ret.get(j);
        }
        return r;
    }

    public static String[] parameterDescriptors(Method m) {
        return parameterDescriptors(m.getParameterTypes());
    }

    public static String[] parameterDescriptors(Class<?>[] parameters) {
        String[] ret = new String[parameters.length];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = DescriptorUtils.makeDescriptor(parameters[i]);
        }
        return ret;
    }

    public static String returnType(String methodDescriptor) {
        return methodDescriptor.substring(methodDescriptor.lastIndexOf(')') + 1, methodDescriptor.length());
    }


    /**
     * returns true if the descriptor represents a primitive type
     */
    public static boolean isPrimitive(String descriptor) {
        if (descriptor.length() == 1) {
            return true;
        }
        return false;
    }

    /**
     * returns true if the descriptor represents a long or a double
     */
    public static boolean isWide(String descriptor) {
        if (!isPrimitive(descriptor)) {
            return false;
        }
        char c = descriptor.charAt(0);
        if (c == 'D' || c == 'J') {
            return true;
        }
        return false;
    }

    /**
     * returns true if the class represents a long or a double
     */
    public static boolean isWide(Class<?> cls) {
        return cls == double.class || cls == long.class;
    }

    public static String methodDescriptor(Method m) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            desc.append(DescriptorUtils.makeDescriptor(p));
        }
        desc.append(")");
        desc.append(DescriptorUtils.makeDescriptor(m.getReturnType()));
        return desc.toString();
    }

    public static String methodDescriptor(String[] parameters, String returnType) {
        StringBuilder desc = new StringBuilder("(");
        for (String p : parameters) {
            desc.append(p);
        }
        desc.append(")");
        desc.append(returnType);
        return desc.toString();
    }

    /**
     * performs basic validation on a descriptor
     */
    public static String validateDescriptor(String descriptor) {
        if (descriptor.length() == 0) {
            throw new RuntimeException("descriptors may not be empty");
        }
        if (descriptor.length() > 1) {
            if (descriptor.startsWith("L")) {
                if (!descriptor.endsWith(";")) {
                    throw new RuntimeException(descriptor + " is not a valid descriptor");
                }
            } else if (descriptor.startsWith("[")) {

            } else {
                throw new RuntimeException(descriptor + " is not a valid descriptor");
            }
        } else {
            char type = descriptor.charAt(0);
            switch (type) {
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'F':
                case 'D':
                case 'V':
                case 'J':
                case 'C':
                    break;
                default:
                    throw new RuntimeException(descriptor + " is not a valid descriptor");
            }
        }
        return descriptor;
    }
}
