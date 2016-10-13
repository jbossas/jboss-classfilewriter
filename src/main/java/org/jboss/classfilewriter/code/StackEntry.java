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
        if(descriptor != null && descriptor.contains(".")) {
            throw new RuntimeException("invalid descriptor "+ descriptor);
        }
        this.descriptor = descriptor;
        this.newInstructionLocation = -1;
        this.descriptorIndex = -1;
    }

    public StackEntry(StackEntryType type, String descriptor, ConstPool pool) {
        this.type = type;
        this.descriptor = descriptor;
        this.newInstructionLocation = -1;
        if(descriptor != null && descriptor.contains(".")) {
            throw new RuntimeException("invalid descriptor " + descriptor);
        }
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
        if(descriptor != null && descriptor.contains(".")) {
            throw new RuntimeException("invalid descriptor " + descriptor);
        }
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
        String desc = descriptor.replace(".", "/");
        if(!DescriptorUtils.isPrimitive(desc)) {
            return new StackEntry(StackEntryType.OBJECT, desc, pool);
        }else {
            char ret = desc.charAt(0);
            switch (ret) {
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'C':
                    return new StackEntry(StackEntryType.INT, desc);
                case 'F':
                    return new StackEntry(StackEntryType.FLOAT, desc);
                case 'D':
                    return new StackEntry(StackEntryType.DOUBLE, desc);
                case 'J':
                    return new StackEntry(StackEntryType.LONG, desc);
            }
        }
        throw new RuntimeException("Unknown descriptor: " + desc);
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
