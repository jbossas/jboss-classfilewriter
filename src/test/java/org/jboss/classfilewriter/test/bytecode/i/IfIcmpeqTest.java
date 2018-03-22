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
package org.jboss.classfilewriter.test.bytecode.i;

import org.junit.Assert;

import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class IfIcmpeqTest {

    public static int value = 10;

    @Test
    public void testIfAcmpeq() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class, int.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);
        ca.iload(1);
        BranchEnd end = ca.ifIcmpeq();
        ca.iconst(10);
        ca.returnInstruction();
        ca.branchEnd(end);
        ca.iconst(0);
        ca.returnInstruction();
        Assert.assertEquals(0, (int) mt.invoke(10, 10));
        Assert.assertEquals(10, (int) mt.invoke(10, 11));
    }
}
