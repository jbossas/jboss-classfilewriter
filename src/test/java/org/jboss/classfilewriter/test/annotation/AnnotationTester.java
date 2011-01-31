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
import java.lang.reflect.Method;

import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;

/**
 * Utility class for testing annotation bytecode
 * 
 * @author Stuart Douglas
 * 
 */
public class AnnotationTester {

    private static int count = 0;

    private static final String NAME = "com.test.AnnotationTest";

    public static Field testFieldAnnotations(Class<?> clazz, String name) {
        try {

            ClassFile file = new ClassFile(NAME + count++, Object.class.getName());
            Field field = clazz.getDeclaredField(name);
            file.addField(field);
            Class<?> newClass = file.define(clazz.getClassLoader());
            return newClass.getDeclaredField(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method testMethodAnnotations(Class<?> clazz, String name) {
        try {
            ClassFile file = new ClassFile(NAME + count++, Object.class.getName());
            Method method = clazz.getDeclaredMethod(name, String.class);
            ClassMethod cmeth = file.addMethod(method);
            cmeth.getCodeAttribute().returnInstruction();
            Class<?> newClass = file.define(clazz.getClassLoader());
            return newClass.getDeclaredMethod(name, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
