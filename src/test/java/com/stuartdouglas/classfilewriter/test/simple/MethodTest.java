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
package com.stuartdouglas.classfilewriter.test.simple;

import java.lang.reflect.Method;

import junit.framework.Assert;

import org.junit.Test;

import com.stuartdouglas.classfilewriter.AccessFlag;
import com.stuartdouglas.classfilewriter.ClassFile;
public class MethodTest {

    @Test
    public void testCreatingMethod() throws SecurityException, NoSuchMethodException {


        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT), "method1", "Ljava/lang/Object;", "I", "J");
        test.addMethod(AccessFlag.of(AccessFlag.PUBLIC), "method2", "V").getCodeAttribute().returnInstruction();

        Class<?> clazz = test.define(getClass().getClassLoader());
        Assert.assertEquals(getClass().getName() + "GEN", clazz.getName());

        Method method1 = clazz.getDeclaredMethod("method1", int.class, long.class);
        Assert.assertEquals(Object.class, method1.getReturnType());
        Assert.assertEquals(Object.class, method1.getGenericReturnType());
        Assert.assertEquals(2, method1.getParameterTypes().length);
        Assert.assertEquals(int.class, method1.getParameterTypes()[0]);
        Assert.assertEquals(long.class, method1.getParameterTypes()[1]);
        Assert.assertEquals("method1", method1.getName());

        Method method2 = clazz.getDeclaredMethod("method2");

    }

    public class AA {

    }

}
