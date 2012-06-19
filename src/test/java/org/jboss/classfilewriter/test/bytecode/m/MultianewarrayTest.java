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
package org.jboss.classfilewriter.test.bytecode.m;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class MultianewarrayTest {

    @Test
    public void testMultianewarray() {
        // TODO: test this better
        MethodTester<Object> mt = new MethodTester<Object>(Object.class, Object.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iconst(3);
        ca.iconst(4);
        ca.multianewarray("java.lang.Object", 2);
        ca.returnInstruction();
        Object result = mt.invoke(this);
        Assert.assertTrue(result.getClass().isArray());
        Object[][] array = (Object[][]) result;
        Assert.assertEquals(3, array.length);
        Assert.assertEquals(4, array[0].length);

    }

}
