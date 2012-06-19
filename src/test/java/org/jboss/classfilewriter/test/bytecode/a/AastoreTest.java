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
package org.jboss.classfilewriter.test.bytecode.a;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class AastoreTest {

    public static final Class<?>[] VALUE = { null, null, null };

    @Test
    public void testAastore() {
        MethodTester<Void> mt = new MethodTester<Void>(void.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.getstatic(getClass().getName(), "VALUE", "[Ljava/lang/Class;");
        ca.iconst(1);
        ca.loadType("Ljava/lang/Integer;");
        ca.aastore();

        ca.returnInstruction();
        mt.invoke();
        Assert.assertEquals(Integer.class, VALUE[1]);
    }
}
