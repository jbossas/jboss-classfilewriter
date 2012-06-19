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
package org.jboss.classfilewriter.test.bytecode.d;

import junit.framework.Assert;

import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class Dup2X2Test {

    @Test
    public void dup2X2Test() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iconst(0);
        ca.iconst(0);
        ca.iconst(1);
        ca.iconst(2);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(2, (int) mt.invoke());
    }

    @Test
    public void dup2X2WideTest() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dconst(0);
        ca.dconst(0);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(0.0, (double) mt.invoke());
    }

    @Test
    public void dup2X2Wide2Test() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iconst(0);
        ca.iconst(0);
        ca.dconst(0);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(0.0, (double) mt.invoke());
    }

    @Test
    public void dup2X2Wide3Test() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dconst(0);
        ca.iconst(0);
        ca.iconst(1);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(1, (int) mt.invoke());
    }

    @Test(expected = InvalidBytecodeException.class)
    public void dup2X2Wide4Test() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dconst(0);
        ca.dconst(0);
        ca.iconst(1);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(0, (double) mt.invoke());
    }

    @Test(expected = InvalidBytecodeException.class)
    public void dup2X2Wide5Test() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dconst(0);
        ca.dconst(0);
        ca.iconst(1);
        ca.dconst(0);
        ca.dup2X2();
        ca.pop2();
        ca.pop2();
        ca.returnInstruction();
        Assert.assertEquals(0, (double) mt.invoke());
    }
}
