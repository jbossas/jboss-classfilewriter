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
package com.stuartdouglas.classfilewriter.attributes;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;

import com.stuartdouglas.classfilewriter.ClassMethod;
import com.stuartdouglas.classfilewriter.code.CodeAttribute;
import com.stuartdouglas.classfilewriter.code.StackEntry;
import com.stuartdouglas.classfilewriter.code.StackFrame;
import com.stuartdouglas.classfilewriter.constpool.ConstPool;

/**
 * A JDK 6 StackMap sttribute.
 *
 * @author Stuart Douglas
 *
 */
public class StackMapAttribute extends Attribute {

    private static int FULL_FRAME = 255;

    public static final String NAME = "StackMap";

    private final ClassMethod method;

    public StackMapAttribute(ClassMethod classMethod, ConstPool constPool) {
        super(NAME, constPool);
        method = classMethod;
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        // as we don't know the size yet we write everything to a byte stream first
        // TODO: make this better
        final CodeAttribute ca = method.getCodeAttribute();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dstream = new DataOutputStream(bout);
        // now we need to write the stack frames.
        // for now we are going to write all frames as full frames
        // TODO: optimise the frame creation
        int lastPos = -1;
        for (Entry<Integer, StackFrame> entry : method.getCodeAttribute().getStackFrames().entrySet()) {
            int offset = entry.getKey() - lastPos - 1;
            lastPos = entry.getKey();
            writeFullFrame(dstream, offset, lastPos, entry.getValue());
        }

        // write to dstream
        stream.writeInt(bout.size() + 2);
        stream.writeShort(ca.getStackFrames().size());
        stream.write(bout.toByteArray());
    }

    /**
     * writes a full frame to the stack map table
     */
    private void writeFullFrame(DataOutputStream dstream, int offset, int position, StackFrame value) throws IOException {
        dstream.writeByte(FULL_FRAME);
        dstream.writeShort(offset);
        dstream.writeShort(value.getLocalVariableState().getContents().size());
        for (StackEntry i : value.getLocalVariableState().getContents()) {
            i.write(dstream, position);
        }
        dstream.writeShort(value.getStackState().getContents().size());
        for (StackEntry i : value.getLocalVariableState().getContents()) {
            i.write(dstream, position);
        }
    }

}
