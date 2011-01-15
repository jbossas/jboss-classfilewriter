/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.classfilewriter.test.bytecode;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.junit.Test;

public class FaddTest {

    @Test
    public void faddTest() {
        MethodTester<Float> mt = new MethodTester<Float>(float.class, float.class, float.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.fload(0);
        ca.fload(1);
        ca.fadd();
        ca.returnInstruction();
        Assert.assertEquals(2.0f, (float) mt.invoke(1.0f, 1.0f));
    }

}
