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

import junit.framework.Assert;

import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class IfAcmpneTest {

    public static Integer value = new Integer(10);

    @Test
    public void testIfAcmpne() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class, Integer.class, Integer.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.aload(0);
        ca.aload(1);
        BranchEnd end = ca.ifAcmpne();
        ca.iconst(10);
        ca.returnInstruction();
        ca.branchEnd(end);
        ca.iconst(0);
        ca.returnInstruction();
        Assert.assertEquals(10, (int) mt.invoke(value, value));
        Assert.assertEquals(0, (int) mt.invoke(value, new Integer(11)));
    }
}
