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

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.jboss.classfilewriter.test.bytecode.SomeInterface;
import org.junit.Test;

public class InvokeinterfaceTest implements SomeInterface {

    private int value = 0;

    @Test
    public void invokeinterfaceTest() {
        MethodTester<Void> mt = new MethodTester<Void>(void.class, InvokeinterfaceTest.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.aload(0);
        ca.iconst(100);
        ca.invokeinterface(SomeInterface.class.getName(), "setInt", "(I)V");
        ca.returnInstruction();
        mt.invoke(this);
        Assert.assertEquals(100, value);
    }

    @Test
    public void invokeinterfaceLongTest() {
        MethodTester<Long> mt = new MethodTester<Long>(long.class, InvokeinterfaceTest.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.aload(0);
        ca.lconst(10);
        ca.invokeinterface(SomeInterface.class.getName(), "incrementLong", "(J)J");
        ca.returnInstruction();
        Assert.assertEquals(11L, (long) mt.invoke(this));
    }

    public long incrementLong(long value) {
        return value + 1;
    }

    public void setInt(int v) {
        value = v;
    }

}
