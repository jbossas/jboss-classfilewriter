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

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class DstoreTest {

    @Test
    public void testDtore() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dconst(1.0);
        ca.dstore(0);
        ca.dload(0);
        ca.returnInstruction();
        Assert.assertEquals(1.0, (double) mt.invoke());
    }
}
