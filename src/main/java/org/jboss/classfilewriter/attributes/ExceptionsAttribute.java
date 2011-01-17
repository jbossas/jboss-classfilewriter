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
package org.jboss.classfilewriter.attributes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * The exceptions attribute, stores the checked exceptions a method is declared to throw
 * 
 * @author Stuart Douglas
 * 
 */
public class ExceptionsAttribute extends Attribute {

    public static final String NAME = "Exceptions";

    private final List<String> exceptionClasses = new ArrayList<String>();

    private final List<Short> exceptionClassIndexes = new ArrayList<Short>();

    private final ConstPool constPool;

    public ExceptionsAttribute(ConstPool constPool) {
        super(NAME, constPool);
        this.constPool = constPool;
    }

    public void addExceptionClass(String exception) {
        exceptionClasses.add(exception);
        exceptionClassIndexes.add(constPool.addClassEntry(exception));
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        stream.writeInt(2 + exceptionClassIndexes.size() * 2);
        stream.writeShort(exceptionClassIndexes.size());
        for (short i : exceptionClassIndexes) {
            stream.writeShort(i);
        }
    }

}
