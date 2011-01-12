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
package com.stuartdouglas.classfilewriter;

import java.io.DataOutputStream;
import java.io.IOException;

import com.stuartdouglas.classfilewriter.constpool.ConstPool;

public class ClassField implements WritableEntry {

    private short accessFlags;
    private final String name;
    private final short nameIndex;
    private final String descriptor;
    private final short descriptorIndex;

    private final ClassFile classFile;

    public ClassField(short accessFlags, String name, String descriptor, ClassFile classFile, ConstPool constPool) {
        this.accessFlags = accessFlags;
        this.name = name;
        this.descriptor = descriptor;
        this.classFile = classFile;
        this.nameIndex = constPool.addUtf8Entry(name);
        this.descriptorIndex = constPool.addUtf8Entry(descriptor);
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(accessFlags);
        stream.writeShort(nameIndex);
        stream.writeShort(descriptorIndex);
        stream.writeShort(0);// no attributes
    }

    public short getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(short accessFlags) {
        this.accessFlags = accessFlags;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }
}
