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

import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.junit.Test;

public class IfIcmpeqTest {

    public static int value = 10;

    @Test
    public void testIfAcmpeq() {
        MethodTester<Integer> mt = new MethodTester<Integer>(int.class, int.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);
        ca.iload(1);
        BranchEnd end = ca.ifIcmpeq();
        ca.iconst(10);
        ca.returnInstruction();
        ca.branchEnd(end);
        ca.iconst(0);
        ca.returnInstruction();
        Assert.assertEquals(0, (int) mt.invoke(10, 10));
        Assert.assertEquals(10, (int) mt.invoke(10, 11));
    }
}
