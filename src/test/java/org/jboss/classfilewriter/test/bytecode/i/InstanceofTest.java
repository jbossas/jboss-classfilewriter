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
package org.jboss.classfilewriter.test.bytecode.i;

import java.io.ByteArrayInputStream;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class InstanceofTest {


    @Test
    public void testInstanceof() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class, Object.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.aload(0);
        ca.instanceofInstruction("java.io.InputStream");
        ca.returnInstruction();
        Assert.assertEquals(0, (int) mt.invoke(this));
        Assert.assertEquals(1, (int) mt.invoke(new ByteArrayInputStream(new byte[0])));
    }
}
