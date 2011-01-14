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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.junit.Test;

public class FieldTest {

    public Map<String, Integer> mapField;

    @Test
    public void testCreatingField() throws SecurityException, NoSuchFieldException {

        Field mapField = getClass().getDeclaredField("mapField");

        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        test.addField(AccessFlag.PUBLIC, "field1", "I");
        test.addField(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.STATIC), "field2", "Ljava/lang/Object;");
        test.addField(AccessFlag.PUBLIC, "field3", AA.class);
        test.addField(AccessFlag.PUBLIC, "field4", mapField.getType(), mapField.getGenericType());

        Class<?> clazz = test.define(getClass().getClassLoader());
        Assert.assertEquals(getClass().getName() + "GEN", clazz.getName());

        Field field1 = clazz.getDeclaredField("field1");
        Assert.assertEquals(int.class, field1.getType());
        Assert.assertEquals(int.class, field1.getGenericType());
        Assert.assertEquals("field1", field1.getName());

        Field field2 = clazz.getDeclaredField("field2");
        Assert.assertEquals(Object.class, field2.getType());
        Assert.assertEquals(Object.class, field2.getGenericType());
        Assert.assertEquals("field2", field2.getName());
        Assert.assertTrue(Modifier.isStatic(field2.getModifiers()));

        Field field3 = clazz.getDeclaredField("field3");
        Assert.assertEquals(AA.class, field3.getType());
        Assert.assertEquals(AA.class, field3.getGenericType());
        Assert.assertEquals("field3", field3.getName());


        Field field4 = clazz.getDeclaredField("field4");
        Assert.assertEquals(Map.class, field4.getType());
        Assert.assertTrue(field4.getGenericType() instanceof ParameterizedType);
        ParameterizedType field4type = (ParameterizedType) field4.getGenericType();
        Assert.assertEquals(Map.class,field4type.getRawType());
        Assert.assertEquals(String.class, field4type.getActualTypeArguments()[0]);
        Assert.assertEquals(Integer.class,field4type.getActualTypeArguments()[1]);

        Assert.assertEquals("field4", field4.getName());

    }

    public class AA {

    }

}
