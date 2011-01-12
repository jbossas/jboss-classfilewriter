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

import java.lang.reflect.Field;

import junit.framework.Assert;

import org.junit.Test;

import com.stuartdouglas.classfilewriter.AccessFlag;
import com.stuartdouglas.classfilewriter.ClassFile;
public class FieldTest {

    @Test
    public void testCreatingField() throws SecurityException, NoSuchFieldException {

        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        test.addField("field1", "I", AccessFlag.PUBLIC);
        test.addField("field2", "Ljava/lang/Object;", AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.STATIC));

        Class<?> clazz = test.define(getClass().getClassLoader());
        Assert.assertEquals(getClass().getName() + "GEN", clazz.getName());

        Field field1 = clazz.getDeclaredField("field1");
        Assert.assertEquals(int.class, field1.getType());
        Assert.assertEquals("field1", field1.getName());

        Field field2 = clazz.getDeclaredField("field2");
        Assert.assertEquals(Object.class, field2.getType());
        Assert.assertEquals("field2", field2.getName());

    }

}
