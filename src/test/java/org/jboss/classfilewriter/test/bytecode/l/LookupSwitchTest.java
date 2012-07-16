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
package org.jboss.classfilewriter.test.bytecode.l;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;
import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.code.LookupSwitchBuilder;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class LookupSwitchTest {

    public static int value = 10;

    @Test
    public void testLookupSwitch1() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);

        final LookupSwitchBuilder builder = new LookupSwitchBuilder();
        final AtomicReference<BranchEnd> v20 = builder.add(20);
        final AtomicReference<BranchEnd> v100 = builder.add(100);
        ca.lookupswitch(builder);
        ca.branchEnd(builder.getDefaultBranchEnd().get());
        ca.iconst(1);
        ca.returnInstruction();
        ca.branchEnd(v20.get());
        ca.iconst(21);
        ca.returnInstruction();
        ca.branchEnd(v100.get());
        ca.iconst(101);
        ca.returnInstruction();
        Assert.assertEquals(1, (int) mt.invoke(0));
        Assert.assertEquals(1, (int) mt.invoke(10));
        Assert.assertEquals(21, (int) mt.invoke(20));
        Assert.assertEquals(101, (int) mt.invoke(100));
    }
}
