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
package org.jboss.classfilewriter.test.simple;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import junit.framework.Assert;
import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.DuplicateMemberException;
import org.junit.Test;

public class FieldTest<T extends Object, TT extends Object, TTT extends Object> {

    public Map<TT, Integer> mapField;

    @Test
    public void testCreatingField() throws SecurityException, NoSuchFieldException {

        Field mapField = getClass().getDeclaredField("mapField");

        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "GEN", "java/lang/Object");
        test.addField(AccessFlag.PUBLIC, "field1", "I");
        test.addField(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.STATIC), "field2", "Ljava/lang/Object;");
        test.addField(AccessFlag.PUBLIC, "field3", AA.class);
        test.addField(AccessFlag.PUBLIC, "field4", mapField.getType());

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
        // Assert.assertTrue(field4.getGenericType() instanceof ParameterizedType);
        // ParameterizedType field4type = (ParameterizedType) field4.getGenericType();
        // Assert.assertEquals(Map.class,field4type.getRawType());
        // Assert.assertEquals(String.class, field4type.getActualTypeArguments()[0]);
        // Assert.assertEquals(Integer.class,field4type.getActualTypeArguments()[1]);

        Assert.assertEquals("field4", field4.getName());

    }

    @Test(expected = DuplicateMemberException.class)
    public void testDuplicateField() {
        ClassFile test = new ClassFile(getClass().getName().replace('.', '/') + "DuplicateField", "java/lang/Object");
        test.addField(AccessFlag.PUBLIC, "field1", "I");
        test.addField(AccessFlag.PUBLIC, "field1", "I");
    }

    public class AA {

    }

}
