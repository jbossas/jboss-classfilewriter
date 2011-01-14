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
package org.jboss.classfilewriter.test.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.DescriptorUtils;

/**
 * Utility class for testing method bytecode
 * 
 * @author Stuart Douglas
 * 
 */
public class MethodTester<T> {

    private static int methodNo;

    private final ClassMethod method;

    private boolean created = false;

    private Method createdMethod;

    public MethodTester(Class<T> returnType, Class<?>... params) {
        ClassFile file = new ClassFile("org.jboss.classWriter" + methodNo++, "java.lang.Object");
        String[] nparams = new String[params.length];
        for(int i = 0; i < params.length;++i){
            nparams[i] = DescriptorUtils.classToStringRepresentation(params[i]);
        }
        method = file.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.STATIC), "method", DescriptorUtils
                .classToStringRepresentation(returnType), nparams);
    }

    public CodeAttribute getCodeAttribute() {
        return method.getCodeAttribute();
    }

    public T invoke(Object... params) {
        try {
            return (T) createdMethod.invoke(null, params);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Method getMethod() {
        if (!created) {
            create();
        }
        return createdMethod;
    }

    private void create() {
        created = true;
        Class<?> clazz = method.getClassFile().define(getClass().getClassLoader());
        for (Method i : clazz.getDeclaredMethods()) {
            if (i.getName().equals("method")) {
                createdMethod = i;
                break;
            }
        }
        if (createdMethod == null) {
            throw new RuntimeException("Created method not found on class");
        }
    }

}
