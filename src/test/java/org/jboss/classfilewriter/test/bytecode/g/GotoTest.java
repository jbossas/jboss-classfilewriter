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
package org.jboss.classfilewriter.test.bytecode.g;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class GotoTest {

    public static Integer value = null;

    @Test
    public void gotoTest() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.getstatic(getClass().getName(), "value", "Ljava/lang/Integer;");
        BranchEnd end = ca.ifnull();
        ca.iconst(10);
        BranchEnd gotoEnd = ca.gotoInstruction();
        ca.branchEnd(end);
        ca.iconst(0);
        ca.branchEnd(gotoEnd);
        ca.returnInstruction();
        Assert.assertEquals(0, (int) mt.invoke());
        value = 1;
        Assert.assertEquals(10, (int) mt.invoke());
    }
}
