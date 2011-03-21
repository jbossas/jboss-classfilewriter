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

import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.code.StackEntry;
import org.jboss.classfilewriter.code.StackEntryType;
import org.jboss.classfilewriter.code.StackFrame;
import org.jboss.classfilewriter.code.StackFrameType;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;
import org.jboss.classfilewriter.util.LazySize;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * A JDK 6 StackMap sttribute.
 *
 *TODO: this will currently fall over if the code length, max locals or max stack is above 65535
 *
 * @author Stuart Douglas
 *
 */
public class StackMapTableAttribute extends Attribute {

    private static int FULL_FRAME = 255;
    private static int SAME_FRAME_EXTENDED = 251;


    public static final String NAME = "StackMapTable";

    private final ClassMethod method;

    public StackMapTableAttribute(ClassMethod classMethod, ConstPool constPool) {
        super(NAME, constPool);
        method = classMethod;
    }

    @Override
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {
        // as we don't know the size yet we write everything to a byte stream first
        // TODO: make this better
        final CodeAttribute ca = method.getCodeAttribute();
        // now we need to write the stack frames.
        // for now we are going to write all frames as full frames
        // TODO: optimise the frame creation

        // write to dstream
        LazySize size = stream.writeSize();
        stream.writeShort(ca.getStackFrames().size());
        int lastPos = -1;
        for (Entry<Integer, StackFrame> entry : method.getCodeAttribute().getStackFrames().entrySet()) {
            int offset = entry.getKey() - lastPos - 1;
            lastPos = entry.getKey();
            StackFrame frame = entry.getValue();
            if (frame.getType() == StackFrameType.SAME_FRAME || frame.getType() == StackFrameType.SAME_FRAME_EXTENDED) {
                writeSameFrame(stream, offset, lastPos, frame);
            } else if (frame.getType() == StackFrameType.SAME_LOCALS_1_STACK && offset < (127 - 64)) {
                writeSameLocals1Stack(stream, offset, lastPos, frame);
            } else {
                writeFullFrame(stream, offset, lastPos, entry.getValue());
            }
        }
        size.markEnd();
    }

    private void writeSameLocals1Stack(DataOutputStream dstream, int offset, int lastPos, StackFrame frame) throws IOException {
        dstream.writeByte(offset + 64);
        frame.getStackState().getContents().get(0).write(dstream);
    }

    private void writeSameFrame(DataOutputStream dstream, int offset, int lastPos, StackFrame frame) throws IOException {
        if (offset > 63) {
            dstream.writeByte(SAME_FRAME_EXTENDED);
            dstream.writeShort(offset);
        } else {
            dstream.writeByte(offset);
        }
    }

    /**
     * writes a full_frame to the stack map table
     */
    private void writeFullFrame(DataOutputStream dstream, int offset, int position, StackFrame value) throws IOException {
        dstream.writeByte(FULL_FRAME);
        dstream.writeShort(offset);
        List<StackEntry> realLocalVars = new ArrayList<StackEntry>(value.getLocalVariableState().getContents().size());
        for (StackEntry i : value.getLocalVariableState().getContents()) {
            if (i.getType() != StackEntryType.TOP) {
                realLocalVars.add(i);
            }
        }
        dstream.writeShort(realLocalVars.size());
        for (StackEntry i : realLocalVars) {
            i.write(dstream);
        }
        // TODO: this is inefficient, the stack should store the number of TOP values in each frame
        List<StackEntry> realStack = new ArrayList<StackEntry>(value.getStackState().getContents().size());
        for (StackEntry i : value.getStackState().getContents()) {
            if (i.getType() != StackEntryType.TOP) {
                realStack.add(i);
            }
        }
        dstream.writeShort(realStack.size());
        for (StackEntry i : realStack) {
            i.write(dstream);
        }
    }

}
