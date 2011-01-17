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
package org.jboss.classfilewriter.test.simple;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import junit.framework.Assert;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.junit.Test;

public class MethodTest {

    @Test
    public void testCreatingMethod() throws SecurityException, NoSuchMethodException, IOException {


        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT), "method1", "Ljava/lang/Object;", "I", "J");
        CodeAttribute code = test.addMethod(AccessFlag.of(AccessFlag.PUBLIC), "method2", "V").getCodeAttribute();
        code.ldc(100);
        code.iconst(500);
        code.ldc(1);
        code.iconst(1);
        code.pop();
        code.pop2();
        code.returnInstruction();


        FileOutputStream s = new FileOutputStream("/tmp/MyFile1.class");
        s.write(test.toBytecode());
        s.close();
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

    @Test
    public void testExceptionTypes() throws SecurityException, NoSuchMethodException {

        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "ExceptionTypes", "java/lang/Object");
        test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT), "method", "Ljava/lang/Object;", "I", "J")
                .addCheckedExceptions(Exception.class);
        Class<?> clazz = test.define(getClass().getClassLoader());

        Method method = clazz.getDeclaredMethod("method", int.class, long.class);
        Assert.assertEquals(1, method.getExceptionTypes().length);
        Assert.assertEquals(Exception.class, method.getExceptionTypes()[0]);
    }

    public class AA {

    }

}
