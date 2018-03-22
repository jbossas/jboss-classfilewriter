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
package org.jboss.classfilewriter.test.bytecode.handler;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.code.ExceptionHandler;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

import org.junit.Assert;

public class ExceptionHandlerTest {
    public static Integer[] VALUE;

    @Test
    public void testWithNpe() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ExceptionHandler handler = ca.exceptionBlockStart("java/lang/RuntimeException");
        ca.getstatic(getClass().getName(), "VALUE", "[Ljava/lang/Integer;");
        ca.arraylength();
        ca.returnInstruction();
        ca.exceptionBlockEnd(handler);
        ca.exceptionHandlerStart(handler);
        ca.iconst(1);
        ca.returnInstruction();
        Assert.assertEquals(1, (int) mt.invoke());
    }

    @Test
    public void testNotCalled() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ExceptionHandler handler = ca.exceptionBlockStart("java/lang/RuntimeException");
        ca.iconst(200);
        ca.returnInstruction();
        ca.exceptionBlockEnd(handler);
        ca.exceptionHandlerStart(handler);
        ca.iconst(20);
        ca.returnInstruction();
        Assert.assertEquals(200, (int) mt.invoke());
    }

    @Test
    public void testNonInternalBinaryName() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ExceptionHandler handler = ca.exceptionBlockStart("java.lang.RuntimeException");
        ca.getstatic(getClass().getName(), "VALUE", "[Ljava/lang/Integer;");
        ca.arraylength();
        ca.returnInstruction();
        ca.exceptionBlockEnd(handler);
        ca.exceptionHandlerStart(handler);
        ca.iconst(1);
        ca.returnInstruction();
        Assert.assertEquals(1, (int) mt.invoke());
    }

}
