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
package org.jboss.classfilewriter.test.bytecode.m;

import junit.framework.Assert;

import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;

public class MultianewarrayTest {

    @Test
    public void testMultianewarray() {
        // TODO: test this better
        MethodTester<Object> mt = new MethodTester<Object>(Object.class, Object.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iconst(3);
        ca.iconst(4);
        ca.multianewarray("java.lang.Object", 2);
        ca.returnInstruction();
        Object result = mt.invoke(this);
        Assert.assertTrue(result.getClass().isArray());
        Object[][] array = (Object[][]) result;
        Assert.assertEquals(3, array.length);
        Assert.assertEquals(4, array[0].length);

    }

}
