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

public class DsubTest {

    @Test
    public void dsubTest() {
        MethodTester<Double> mt = new MethodTester<Double>(double.class, double.class, double.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.dload(0);
        ca.dload(2);
        ca.dsub();
        ca.returnInstruction();
        Assert.assertEquals(10.0 - 5.0, (double) mt.invoke(10.0, 5.0));
        Assert.assertEquals(12.0 - 2.0, (double) mt.invoke(12.0, 2.0));
    }


}
