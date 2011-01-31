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
package org.jboss.classfilewriter.annotations;

import java.io.DataOutputStream;
import java.io.IOException;

import org.jboss.classfilewriter.WritableEntry;
import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * Represents an annotation name/value pair. This class can also represent a value an an array valued annotation instance, if
 * the name is null
 * 
 * @author Stuart Douglas
 * 
 */
public abstract class AnnotationValue implements WritableEntry {

    private final String name;

    private final int nameIndex;

    protected AnnotationValue(ConstPool constPool, String name) {
        this.name = name;
        if (name != null) {
            this.nameIndex = constPool.addUtf8Entry(name);
        } else {
            this.nameIndex = -1;
        }
    }

    public void write(DataOutputStream stream) throws IOException {
        if (nameIndex != -1) {
            stream.writeShort(nameIndex);
        }
        stream.writeByte(getTag());
        writeData(stream);
    }

    public abstract void writeData(DataOutputStream stream) throws IOException;

    public String getName() {
        return name;
    }

    public abstract char getTag();
}
