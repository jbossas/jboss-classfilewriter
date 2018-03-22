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
package org.jboss.classfilewriter.test.bytecode.n;

import org.junit.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class NewTest {

    @Test
    public void newTest() throws SecurityException, NoSuchMethodException {
        MethodTester<Object> mt = new MethodTester<Object>(Object.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.newInstruction("java.lang.Object");
        ca.dup();
        ca.invokespecial(Object.class.getConstructor());
        ca.returnInstruction();
        Object val = mt.invoke();
        Assert.assertNotNull(val);
    }

}
