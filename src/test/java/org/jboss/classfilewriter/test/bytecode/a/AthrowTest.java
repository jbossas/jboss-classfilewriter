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
package org.jboss.classfilewriter.test.bytecode.a;

import org.junit.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class AthrowTest {

    public static final Integer[] VALUE = { 2, 3, 4 };

    @Test
    public void testAthrow() {
        try {
            MethodTester<Integer> mt = new MethodTester<Integer>(Integer.class, TestException.class);
            CodeAttribute ca = mt.getCodeAttribute();
            ca.aload(0);
            ca.athrow();
            mt.invoke(new TestException());
            Assert.fail();
        } catch (RuntimeException e) {
            // runtime error wrapping IvocationTargetException wrapping TestException
            if (e.getCause().getCause().getClass() != TestException.class) {
                Assert.fail();
            }
        }

    }

    private static class TestException extends RuntimeException {

    }
}
