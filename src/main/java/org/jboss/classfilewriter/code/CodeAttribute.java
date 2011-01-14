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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.attributes.StackMapTableAttribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.DescriptorUtils;


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

    public void aaload() {
        if (getStack().top().getType() != StackEntryType.INT) {
            throw new InvalidBytecodeException("aaload needs an integer on the top of the stack");
        }
        if (!getStack().top_1().getDescriptor().startsWith("[")) {
            throw new InvalidBytecodeException("aaload needs an array in position 2 on the stack");
        }
        writeByte(Opcode.AALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("Ljava/lang/Object;"));
    }

    public void aastore() {
        if (getStack().top_1().getType() != StackEntryType.INT) {
            throw new InvalidBytecodeException("aastore needs an integer in position 2 on the stack");
        }
        if (!getStack().top_2().getDescriptor().startsWith("[")) {
            throw new InvalidBytecodeException("aaload needs an array in position 3 on the stack");
        }
        writeByte(Opcode.AASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    public void aconstNull() {
        writeByte(Opcode.ACONST_NULL);
        currentOffset++;
        advanceFrame(currentFrame.aconstNull());
    }

    public void aload(int no) {
        LocalVariableState locals = getLocalVars();
        if(locals.size()<= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.OBJECT && entry.getType() != StackEntryType.NULL) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables " + locals.toString());
        }

        if(no > 0xFF) {
            //wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.ALOAD);
            writeShort(no);
            currentOffset+=4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.ALOAD_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.ALOAD);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.push(entry));
    }

    public void astore(int no) {
        if (getStack().size() == 0) {
            throw new InvalidBytecodeException("cannot astore when stack is empty");
        }
        StackEntry top = getStack().top();
        if (top.getType() != StackEntryType.OBJECT && top.getType() != StackEntryType.NULL) {
            throw new InvalidBytecodeException("astore requires reference on top of stack: " + getStack().toString());
        }
        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.ASTORE);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.ASTORE_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.ASTORE);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.store(no));
    }

    /**
     * Do not use Descriptor format (e.g. Ljava/lang/Object;)
     *
     */
    public void checkcast(String className) {
        int classIndex = constPool.addClassEntry(className);
        writeByte(Opcode.CHECKCAST);
        writeShort(classIndex);
        currentOffset += 3;
        advanceFrame(currentFrame.replace(className));
    }

    /**
     * Adds the appropriate fconst instruction.
     * <p>
     * note, if the value is not 0, 1, 2 then ldc is used instead
     *
     */
    public void fconst(float value) {
        if (value == 0) {
            writeByte(Opcode.FCONST_0);
        } else if (value == 1) {
            writeByte(Opcode.FCONST_1);
        } else if (value == 2) {
            writeByte(Opcode.FCONST_2);
        } else {
            ldc(value);
            return;
        }
        currentOffset++;
        advanceFrame(currentFrame.push("F"));
    }

    public void getstatic(String className, String field, String descriptor) {
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.GETSTATIC);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.push(descriptor));
    }

    public void putstatic(String className, String field, String descriptor) {
        if (!getStack().isOnTop(descriptor)) {
            throw new InvalidBytecodeException("Attempting to put wrong type into static field. Field:" + className + "."
                    + field + " (" + descriptor + "). Stack State: " + getStack().toString());
        }
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.PUTSTATIC);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
    }

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
     * Adds an ldc instruction for float
     *
     */
    public void ldc(float value) {
        int index = constPool.addFloatEntry(value);
        ldcInternal(index);
        advanceFrame(currentFrame.push("F"));
    }

    /**
     * Adds an ldc instruction for a String
     * <p>
     * To load a class literal using ldc use the @{link #loadType(String)} method.
     *
     */
    public void ldc(String value) {
        int index = constPool.addStringEntry(value);
        ldcInternal(index);
        advanceFrame(currentFrame.push("Ljava/lang/String;"));
    }

    /**
     * Adds an ldc instruction for an int.
     *
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
     * Adds an ldc2_w instruction for double
     *
     */
    public void ldc2(double value) {
        int index = constPool.addDoubleEntry(value);
        writeByte(Opcode.LDC2_W);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.push("D"));
    }

    /**
     * Adds an ldc2_w instruction for long
     *
     */
    public void ldc2(long value) {
        int index = constPool.addLongEntry(value);
        writeByte(Opcode.LDC2_W);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.push("J"));
    }

    /**
     * Loads a java.lang.Class for the given descriptor into the stack.
     */
    public void loadType(String descriptor) {
        if (descriptor.length() != 1) {
            if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                descriptor = descriptor.substring(1, descriptor.length() - 1);
            }
            loadClass(descriptor);
        } else {
            char type = descriptor.charAt(0);
            switch (type) {
                case 'I':
                    getstatic(Integer.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'J':
                    getstatic(Long.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'S':
                    getstatic(Short.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'F':
                    getstatic(Float.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'D':
                    getstatic(Double.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'B':
                    getstatic(Byte.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'C':
                    getstatic(Character.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                case 'Z':
                    getstatic(Boolean.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
            }
        }
    }

    public void loadClass(String className) {
        int index = constPool.addClassEntry(className);
        ldcInternal(index);
        advanceFrame(currentFrame.push("Ljava/lang/Class;"));
    }

    public void pop() {
        writeByte(Opcode.POP);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void pop2() {
        writeByte(Opcode.POP2);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    /**
     * Adds the appropriate return instruction for the methods return type.
     */
    public void returnInstruction() {
        String returnType = method.getReturnType();
        if (!returnType.equals("V")) {
            if (!getStack().isOnTop(returnType)) {
                throw new InvalidBytecodeException(returnType + " is not on top of stack");
            }
        }

        // all these instructions are one byte
        currentOffset++;

        // return instructions do not create stack map entries

        if (returnType.length() > 1) {
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
        if (getStack().getContents().size() > maxStackDepth) {
            maxStackDepth = getStack().getContents().size();
        }
        if (getLocalVars().getContents().size() > maxLocals) {
            maxLocals = getLocalVars().getContents().size();
        }
    }

    private LocalVariableState getLocalVars() {
        return currentFrame.getLocalVariableState();
    }

    private StackState getStack() {
        return currentFrame.getStackState();
    }

}
