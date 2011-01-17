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
package org.jboss.classfilewriter.code;

import java.io.DataOutputStream;
import java.io.IOException;

import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.DescriptorUtils;

/**
 * represents the state of the stack or the local variable array.
 * <p>
 * This is written out as part of the StackMap attribute
 * 
 * @author Stuart Douglas
 * 
 */
public class StackEntry {

    private final StackEntryType type;

    private final String descriptor;

    /**
     * if this is OBJECT then this holds the const pool index
     */
    private final int descriptorIndex;

    /**
     * if this is an unititialized bytecode then this holds the absolute position of the new instruction that created it
     */
    private final int newInstructionLocation;

    public StackEntry(StackEntryType type, String descriptor) {
        if (type == StackEntryType.OBJECT) {
            throw new RuntimeException("OBJECT stack entries must provide a const pool index for the class");
        }
        this.type = type;
        this.descriptor = descriptor;
        this.newInstructionLocation = -1;
        this.descriptorIndex = -1;
    }

    public StackEntry(StackEntryType type, String descriptor, ConstPool pool) {
        this.type = type;
        this.descriptor = descriptor;
        this.newInstructionLocation = -1;
        if(type == StackEntryType.OBJECT) {
            if (descriptor.charAt(0) == 'L') {
                descriptorIndex = pool.addClassEntry(descriptor.substring(1, descriptor.length() - 1)); // strip the L and the ;
            } else {
                descriptorIndex = pool.addClassEntry(descriptor); // strip the L and the ;
            }

        } else {
            descriptorIndex = -1;
        }
    }

    public StackEntry(StackEntryType type, String descriptor, int newInstructionLocation) {
        this.type = type;
        this.descriptor = descriptor;
        this.newInstructionLocation = newInstructionLocation;
        this.descriptorIndex = -1;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public int getNewInstructionLocation() {
        return newInstructionLocation;
    }

    public StackEntryType getType() {
        return type;
    }

    @Override
    public String toString() {
        if (type == StackEntryType.OBJECT) {
        return "StackEntry [descriptor=" + descriptor + ", type=" + type
                + "]";
        }
        return "StackEntry [type=" + type + "]";
    }

    public static StackEntry of(String descriptor, ConstPool pool) {
        if(!DescriptorUtils.isPrimitive(descriptor)) {
            return new StackEntry(StackEntryType.OBJECT, descriptor, pool);
        }else {
            char ret = descriptor.charAt(0);
            switch (ret) {
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'C':
                    return new StackEntry(StackEntryType.INT, descriptor);
                case 'F':
                    return new StackEntry(StackEntryType.FLOAT, descriptor);
                case 'D':
                    return new StackEntry(StackEntryType.DOUBLE, descriptor);
                case 'J':
                    return new StackEntry(StackEntryType.LONG, descriptor);
            }
        }
        throw new RuntimeException("Unknown descriptor: " + descriptor);
    }

    public boolean isWide() {
        return type == StackEntryType.DOUBLE || type == StackEntryType.LONG;
    }

    /**
     * writes the entry to the stream
     */
    public void write(DataOutputStream dstream) throws IOException {
        dstream.writeByte(type.ordinal());
        if (type == StackEntryType.OBJECT) {
            dstream.writeShort(descriptorIndex);
        } else if (type == StackEntryType.UNITITIALIZED_OBJECT) {
            dstream.writeShort(newInstructionLocation);
        }
    }
}
