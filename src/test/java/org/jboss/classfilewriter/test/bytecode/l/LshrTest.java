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

import org.junit.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class LshrTest {

    @Test
    public void lshlTest() {
        MethodTester<Long> mt = new MethodTester<Long>(long.class, long.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.lload(0);
        ca.iload(2);
        ca.lshr();
        ca.returnInstruction();
        Assert.assertEquals(1000L >> 3, (long) mt.invoke(1000L, 3));
        Assert.assertEquals(12L >> 2, (long) mt.invoke(12L, 2));
    }


}
