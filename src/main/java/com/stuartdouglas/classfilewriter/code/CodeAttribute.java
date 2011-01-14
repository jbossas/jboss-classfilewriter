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
package com.stuartdouglas.classfilewriter.code;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.stuartdouglas.classfilewriter.ClassMethod;
import com.stuartdouglas.classfilewriter.attributes.Attribute;
import com.stuartdouglas.classfilewriter.attributes.StackMapTableAttribute;
import com.stuartdouglas.classfilewriter.constpool.ConstPool;
import com.stuartdouglas.classfilewriter.util.DescriptorUtils;

public class CodeAttribute extends Attribute {

    public static final String NAME = "Code";

    private final ClassMethod method;

    private final ConstPool constPool;

    private final ByteArrayOutputStream finalDataBytes;

    private final DataOutputStream data;

    private int maxLocals = 0;

    private int maxStackDepth = 0;

    private final LinkedHashMap<Integer, StackFrame> stackFrames = new LinkedHashMap<Integer, StackFrame>();

    private StackFrame currentFrame;

    private int currentOffset;

    private final List<Attribute> attributes = new ArrayList<Attribute>();

    public CodeAttribute(ClassMethod method, ConstPool constPool) {
        super(NAME, constPool);
        this.method = method;
        this.constPool = constPool;
        this.finalDataBytes = new ByteArrayOutputStream();
        this.data = new DataOutputStream(finalDataBytes);

        if (!Modifier.isStatic(method.getAccessFlags())) {
            maxLocals++;
        }
        for (String param : method.getParameters()) {
            if (DescriptorUtils.isWide(param)) {
                maxLocals += 2;
            } else {
                maxLocals++;
            }
        }
        // creates a new initial stack frame
        currentFrame = new StackFrame(method);
        stackFrames.put(0, currentFrame);
        currentOffset = 0;
        // add the stack map table
        attributes.add(new StackMapTableAttribute(method, constPool));
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        if (finalDataBytes.size() == 0) {
            throw new RuntimeException("Code attribute is empty for method " + method.getName() + "  " + method.getDescriptor());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        for (Attribute attribute : attributes) {
            attribute.write(dos);
        }

        stream.writeInt(finalDataBytes.size() + 12 + bos.size()); // attribute length
        stream.writeShort(maxStackDepth);
        stream.writeShort(maxLocals);
        stream.writeInt(finalDataBytes.size());
        stream.write(finalDataBytes.toByteArray());
        stream.writeShort(0); // exception table length
        stream.writeShort(attributes.size()); // attributes count
        stream.write(bos.toByteArray());
    }

    // -------------------------------------------
    // Instruction methods, in alphabetical order

    /**
     * Adds the appropriate iconst instruction.
     * <p>
     * note, if the value is not in the range -1 to 5 ldc is written instead
     *
     * @param value
     */
    public void iconst(int value) {
        if (value < -1 || value > 6) {
            ldc(value);
            return;
        }
        writeByte(Opcode.ICONST_0 + value);
        currentOffset++;
        advanceFrame(currentFrame.push("I"));
    }
    /**
     * Adds an ldc instruction for an int.
     *
     * @param value
     */
    public void ldc(int value) {
        if (value > -2 && value < 6) {
            iconst(value);
            return;
        }
        int index = constPool.addIntegerEntry(value);
        ldcInternal(index);
        advanceFrame(currentFrame.push("I"));
    }

    /**
     * Adds an ldc instruction for an int.
     *
     * @param value
     */
    private void ldcInternal(int index) {
        if (index > 0xFF) {
            writeByte(Opcode.LDC_W);
            writeShort(index);
            currentOffset += 3;
        } else {
            writeByte(Opcode.LDC);
            writeByte(index);
            currentOffset += 2;
        }
    }

    /**
     * Adds the appropriate return instruction for the methods return type.
     */
    public void returnInstruction() {
        // all these instructions are one byte
        currentOffset++;

        // return instructions do not create stack map entries

        if (method.getReturnType().length() > 1) {
            writeByte(Opcode.ARETURN);
        } else {
            char ret = method.getReturnType().charAt(0);
            switch (ret) {
                case 'V':
                    writeByte(Opcode.RETURN);
                    break;
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'C':
                    writeByte(Opcode.IRETURN);
                    break;
                case 'F':
                    writeByte(Opcode.FRETURN);
                    break;
                case 'D':
                    writeByte(Opcode.DRETURN);
                    break;
                case 'J':
                    writeByte(Opcode.LRETURN);
            }
        }
    }


    private void writeByte(int n) {
        try {
            data.writeByte(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeShort(int n) {
        try {
            data.writeShort(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LinkedHashMap<Integer, StackFrame> getStackFrames() {
        return new LinkedHashMap<Integer, StackFrame>(stackFrames);
    }

    /**
     * Adds a duplicate of the current frame to the current position.
     * <p>
     * currently this just puts the same frame into a different position
     */
    private void duplicateFrame() {
        stackFrames.put(currentOffset, currentFrame);
        updateMaxValues();
    }

    private void advanceFrame(StackFrame frame) {
        stackFrames.put(currentOffset, frame);
        currentFrame = frame;
        updateMaxValues();
    }

    private void updateMaxValues() {
        if (currentFrame.getStackState().getContents().size() > maxStackDepth) {
            maxStackDepth = currentFrame.getStackState().getContents().size();
        }
        if (currentFrame.getLocalVariableState().getContents().size() > maxLocals) {
            maxLocals = currentFrame.getLocalVariableState().getContents().size();
        }
    }

}
