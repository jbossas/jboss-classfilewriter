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
package org.jboss.classfilewriter.test.signature;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

public class MethodSignatureTest<Y> {

    @Test
    public void methodSignatureTest() throws SecurityException, NoSuchFieldException, NoSuchMethodException {
        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        ClassMethod cmth = test.addMethod(AccessFlag.PUBLIC | AccessFlag.ABSTRACT, "method1", "Ljava/util/List;");
        cmth.setSignature("()Ljava/util/List<Ljava/lang/Integer;>;");
        Class<?> clazz = test.define(getClass().getClassLoader());
        Method method = clazz.getDeclaredMethod("method1");
        ParameterizedType fieldType = (ParameterizedType) method.getGenericReturnType();
        Assert.assertEquals(List.class, fieldType.getRawType());
        Assert.assertEquals(Integer.class, fieldType.getActualTypeArguments()[0]);

    }

}
