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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    /**
     * maps bytecode offsets to jump locations. As these jump locations where not known when the instruction was written they
     * need to be overwritten when the final bytecode is written out
     */
    private final Map<Integer, Integer> jumpLocations = new HashMap<Integer, Integer>();

    private StackFrame currentFrame;

    private int currentOffset;

    private final List<Attribute> attributes = new ArrayList<Attribute>();

    private boolean stackMapAttributeValid = true;

    private final StackMapTableAttribute stackMapTableAttribute;

    private final List<ExceptionHandler> exceptionTable = new ArrayList<ExceptionHandler>();

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
        stackMapTableAttribute = new StackMapTableAttribute(method, constPool);
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {

        if (stackMapAttributeValid) {
            // add the stack map table
            attributes.add(stackMapTableAttribute);
        }

        if (finalDataBytes.size() == 0) {
            throw new RuntimeException("Code attribute is empty for method " + method.getName() + "  " + method.getDescriptor());
        }

        byte[] bytecode = finalDataBytes.toByteArray();
        for (Entry<Integer, Integer> e : jumpLocations.entrySet()) {
            overwriteShort(bytecode, e.getKey(), e.getValue());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        for (Attribute attribute : attributes) {
            attribute.write(dos);
        }

        stream.writeInt(finalDataBytes.size() + 12 + bos.size() + (exceptionTable.size() * 8)); // attribute length
        stream.writeShort(maxStackDepth);
        stream.writeShort(maxLocals);
        stream.writeInt(bytecode.length);
        stream.write(bytecode);
        stream.writeShort(exceptionTable.size()); // exception table length
        for (ExceptionHandler exception : exceptionTable) {
            stream.writeShort(exception.getStart());
            stream.writeShort(exception.getEnd());
            stream.writeShort(exception.getHandler());
            stream.writeShort(exception.getExceptionIndex());
        }
        stream.writeShort(attributes.size()); // attributes count
        stream.write(bos.toByteArray());
    }

    // -------------------------------------------
    // Instruction methods, in alphabetical order

    public void aaload() {
        assertTypeOnStack(StackEntryType.INT, "aaload requires int on top of stack");
        if (!getStack().top_1().getDescriptor().startsWith("[")) {
            throw new InvalidBytecodeException("aaload needs an array in position 2 on the stack");
        }
        writeByte(Opcode.AALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("Ljava/lang/Object;"));
    }

    public void aastore() {
        assertTypeOnStack(StackEntryType.OBJECT, "aastore requires reference type on top of stack");
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

    public void anewarray(String arrayType) {
        assertTypeOnStack(StackEntryType.INT, "anewarray requires int on stack");
        int index = constPool.addClassEntry(arrayType);
        writeByte(Opcode.ANEWARRAY);
        writeShort(index);
        currentOffset += 3;
        if (arrayType.startsWith("[")) {
            advanceFrame(currentFrame.replace("[" + arrayType));
        } else {
            advanceFrame(currentFrame.replace("[L" + arrayType + ";"));
        }
    }

    public void arraylength() {
        assertTypeOnStack(StackEntryType.OBJECT, "arraylength requires array on stack");
        writeByte(Opcode.ARRAYLENGTH);
        currentOffset++;
        advanceFrame(currentFrame.replace("I"));
    }

    public void astore(int no) {
        assertTypeOnStack(StackEntryType.OBJECT, "aastore requires reference type on stack");
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

    public void athrow() {
        assertTypeOnStack(StackEntryType.OBJECT, "athrow requires an object on the stack");
        writeByte(Opcode.ATHROW);
        currentOffset++;
        currentFrame = null;
    }

    public void baload() {
        assertTypeOnStack(StackEntryType.INT, "baload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "baload requires an array in position 2 on the stack");
        writeByte(Opcode.BALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void bastore() {
        assertTypeOnStack(StackEntryType.INT, "bastore requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.INT, "bastore requires an int in position 2 on the stack");
        assertTypeOnStack(2, StackEntryType.OBJECT, "bastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.BASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    public void caload() {
        assertTypeOnStack(StackEntryType.INT, "caload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "caload requires an array in position 2 on the stack");
        writeByte(Opcode.CALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void castore() {
        assertTypeOnStack(StackEntryType.INT, "castore requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.INT, "castore requires an int in position 2 on the stack");
        assertTypeOnStack(2, StackEntryType.OBJECT, "castore requires an array reference in position 3 on the stack");
        writeByte(Opcode.CASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    public void bipush(byte value) {
        writeByte(Opcode.BIPUSH);
        writeByte(value);
        currentOffset += 2;
        advanceFrame(currentFrame.push("B"));
    }

    /**
     * marks the end of a branch. The current stack frame is checked for compatibility with the stack frame at the branch start
     */
    public void branchEnd(BranchEnd end) {
        mergeStackFrames(end.getStackFrame());
        jumpLocations.put(end.getBranchLocation() + 1, currentOffset - end.getBranchLocation());
    }

    /**
     * Do not use Descriptor format (e.g. Ljava/lang/Object;), the correct form is just java/lang/Object or java.lang.Object
     *
     */
    public void checkcast(String className) {
        assertTypeOnStack(StackEntryType.OBJECT, "checkcast requires reference type on stack");
        int classIndex = constPool.addClassEntry(className);
        writeByte(Opcode.CHECKCAST);
        writeShort(classIndex);
        currentOffset += 3;
        advanceFrame(currentFrame.replace(className));
    }

    public void d2f() {
        assertTypeOnStack(StackEntryType.DOUBLE, "d2f requires double on stack");
        writeByte(Opcode.D2F);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("F"));
    }

    public void d2i() {
        assertTypeOnStack(StackEntryType.DOUBLE, "d2i requires double on stack");
        writeByte(Opcode.D2I);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void d2l() {
        assertTypeOnStack(StackEntryType.DOUBLE, "d2l requires double on stack");
        writeByte(Opcode.D2L);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("J"));
    }

    public void dadd() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dadd requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "dadd requires double on stack");
        writeByte(Opcode.DADD);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void daload() {
        assertTypeOnStack(StackEntryType.INT, "daload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "daload requires an array in position 2 on the stack");
        writeByte(Opcode.DALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("D"));
    }

    public void dastore() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dastore requires an int on top of the stack");
        assertTypeOnStack(2, StackEntryType.INT, "dastore requires an int in position 2 on the stack");
        assertTypeOnStack(3, StackEntryType.OBJECT, "dastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.DASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop4());
    }

    public void dcmpg() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dcmpg requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "dcmpg requires double on stack");
        writeByte(Opcode.DCMPG);
        currentOffset++;
        advanceFrame(currentFrame.pop4push1("I"));
    }

    public void dcmpl() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dcmpl requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "dcmpl requires double in position 3 on stack");
        writeByte(Opcode.DCMPL);
        currentOffset++;
        advanceFrame(currentFrame.pop4push1("I"));
    }

    /**
     * Adds the appropriate dconst instruction.
     * <p>
     * note, if the value is not 0 or 1 then ldc is used instead
     *
     */
    public void dconst(double value) {
        if (value == 0.0) {
            writeByte(Opcode.DCONST_0);
        } else if (value == 1.0) {
            writeByte(Opcode.DCONST_1);
        } else {
            ldc2(value);
            return;
        }
        currentOffset++;
        advanceFrame(currentFrame.push("D"));
    }

    public void ddiv() {
        assertTypeOnStack(StackEntryType.DOUBLE, "ddiv requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "ddiv requires double in position 3 on stack");
        writeByte(Opcode.DDIV);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void dload(int no) {
        LocalVariableState locals = getLocalVars();
        if (locals.size() <= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.DOUBLE) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables "
                    + locals.toString());
        }

        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.DLOAD);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.DLOAD_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.DLOAD);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.push(entry));
    }

    public void dmul() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dmul requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "dmul requires double in position 3 on stack");
        writeByte(Opcode.DMUL);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void dneg() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dneg requires double on stack");
        writeByte(Opcode.DNEG);
        currentOffset++;
        duplicateFrame();
    }

    public void drem() {
        assertTypeOnStack(StackEntryType.DOUBLE, "drem requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "drem requires double in position 3 on stack");
        writeByte(Opcode.DREM);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void dstore(int no) {
        assertTypeOnStack(StackEntryType.DOUBLE, "dastore requires double on stack");
        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.DSTORE);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.DSTORE_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.DSTORE);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.store(no));
    }

    public void dsub() {
        assertTypeOnStack(StackEntryType.DOUBLE, "dsub requires double on stack");
        assertTypeOnStack(2, StackEntryType.DOUBLE, "dsub requires double in position 3 on stack");
        writeByte(Opcode.DSUB);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void dup() {
        assertNotWideOnStack("dup acnnot be used if double or long is on top of the stack");
        writeByte(Opcode.DUP);
        currentOffset++;
        advanceFrame(currentFrame.dup());
    }

    public void dupX1() {
        assertNotWideOnStack("dup_x1 cannot be used if double or long is on top of the stack");
        assertNotWideOnStack(1, "dup_x1 cannot be used if double or long is in position 2 on the stack");
        writeByte(Opcode.DUP_X1);
        currentOffset++;
        advanceFrame(currentFrame.dupX1());
    }

    public void dupX2() {
        assertNotWideOnStack("dup_x2 acnnot be used if double or long is on top of the stack");
        writeByte(Opcode.DUP_X2);
        currentOffset++;
        advanceFrame(currentFrame.dupX2());
    }

    public void dup2() {
        writeByte(Opcode.DUP2);
        currentOffset++;
        advanceFrame(currentFrame.dup2());
    }

    public void dup2X1() {
        assertNotWideOnStack(2, "dup2_x1 cannot be used if double or long is in position 3 on the stack");
        writeByte(Opcode.DUP2_X1);
        currentOffset++;
        advanceFrame(currentFrame.dup2X1());
    }

    public void dup2X2() {
        assertNotWideOnStack(3, "dup2_x2 cannot be used if double or long is in position 4 on the stack");
        writeByte(Opcode.DUP2_X2);
        currentOffset++;
        advanceFrame(currentFrame.dup2X2());
    }

    /**
     * Begin writing an exception handler block. The handler is not actually persisted until exceptionHandler is called.
     */
    public ExceptionHandler exceptionHandlerStart(String exceptionType) {
        ExceptionHandler handler = new ExceptionHandler(currentOffset, constPool.addClassEntry(exceptionType), exceptionType,
                currentFrame);
        return handler;
    }

    /**
     * Mark the end of an exception handler block. The last instruction that was written will be the last instruction covered by
     * the handler
     * 
     */
    public void exceptionHandlerEnd(ExceptionHandler handler) {
        handler.setEnd(currentOffset);
    }

    /**
     * Marks the current code location as the exception handler and adds the handler to the exception handler table;
     */
    public void exceptionHandlerAdd(ExceptionHandler handler) {
        if (handler.getEnd() == 0) {
            throw new InvalidBytecodeException(
                    "handler end location must be initialised via exceptionHandlerEnd before calling exceptionHandlerAdd");
        }
        handler.setHandler(currentOffset);
        exceptionTable.add(handler);
        mergeStackFrames(new StackFrame(new StackState(handler.getExceptionType(), constPool), handler.getFrame()
                .getLocalVariableState()));
    }

    public void f2d() {
        assertTypeOnStack(StackEntryType.FLOAT, "f2s requires float on stack");
        writeByte(Opcode.F2D);
        currentOffset++;
        advanceFrame(currentFrame.replace("D"));
    }

    public void f2i() {
        assertTypeOnStack(StackEntryType.FLOAT, "f2i requires float on stack");
        writeByte(Opcode.F2I);
        currentOffset++;
        advanceFrame(currentFrame.replace("I"));
    }

    public void f2l() {
        assertTypeOnStack(StackEntryType.FLOAT, "f2l requires float on stack");
        writeByte(Opcode.F2L);
        currentOffset++;
        advanceFrame(currentFrame.replace("J"));
    }

    public void fadd() {
        assertTypeOnStack(StackEntryType.FLOAT, "fadd requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fadd requires float on stack");
        writeByte(Opcode.FADD);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void faload() {
        assertTypeOnStack(StackEntryType.INT, "faload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "faload requires an array in position 2 on the stack");
        writeByte(Opcode.FALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("F"));
    }

    public void fastore() {
        assertTypeOnStack(StackEntryType.FLOAT, "fastore requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.INT, "fastore requires an int in position 2 on the stack");
        assertTypeOnStack(2, StackEntryType.OBJECT, "fastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.FASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    public void fcmpg() {
        assertTypeOnStack(StackEntryType.FLOAT, "fcmpg requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fcmpg requires float on stack");
        writeByte(Opcode.FCMPG);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void fcmpl() {
        assertTypeOnStack(StackEntryType.FLOAT, "fcmpl requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fcmpl requires float in position 2 on stack");
        writeByte(Opcode.FCMPL);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
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

    public void fdiv() {
        assertTypeOnStack(StackEntryType.FLOAT, "fdiv requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fdiv requires float in position 2 on stack");
        writeByte(Opcode.FDIV);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void fload(int no) {
        LocalVariableState locals = getLocalVars();
        if (locals.size() <= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.FLOAT) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables "
                    + locals.toString());
        }

        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.FLOAD);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.FLOAD_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.FLOAD);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.push(entry));
    }

    public void fmul() {
        assertTypeOnStack(StackEntryType.FLOAT, "fmul requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fmul requires float in position 2 on stack");
        writeByte(Opcode.FMUL);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void fneg() {
        assertTypeOnStack(StackEntryType.FLOAT, "fneg requires float on stack");
        writeByte(Opcode.FNEG);
        currentOffset++;
        duplicateFrame();
    }

    public void frem() {
        assertTypeOnStack(StackEntryType.FLOAT, "frem requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "frem requires float in position 2 on stack");
        writeByte(Opcode.FREM);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void fstore(int no) {
        assertTypeOnStack(StackEntryType.FLOAT, "fstore requires float on stack");
        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.FSTORE);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.FSTORE_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.FSTORE);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.store(no));
    }

    public void fsub() {
        assertTypeOnStack(StackEntryType.FLOAT, "fsub requires float on stack");
        assertTypeOnStack(1, StackEntryType.FLOAT, "fsub requires float in position 2 on stack");
        writeByte(Opcode.FSUB);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void getfield(String className, String field, String descriptor) {
        assertTypeOnStack(StackEntryType.OBJECT, "getfield requires object on stack");
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.GETFIELD);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.replace(descriptor));
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
     * Jump to the given location if the reference type on the top of the stack is null
     */
    public void ifnull(CodeLocation location) {
        assertTypeOnStack(StackEntryType.OBJECT, "ifnull requires reference type on stack");
        writeByte(Opcode.IFNULL);
        writeShort(location.getLocation() - currentOffset);
        mergeStackFrames(location.getStackFrame());
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
    }

    /**
     * Jump to the given location if the reference type on the top of the stack is null.
     * <p>
     * The {@link BranchEnd} returned from this method is used to set the end point to a future point in the bytecode stream
     */
    public BranchEnd ifnull() {
        assertTypeOnStack(StackEntryType.OBJECT, "ifnull requires reference type on stack");
        writeByte(Opcode.IFNULL);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
        BranchEnd ret = new BranchEnd(currentOffset - 3, currentFrame);
        return ret;
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

    /**
     * Gets the location object for the current location in the bytecode. Jumps to this location will begin executing the next
     * instruction that is written to the bytecode stream
     *
     */
    public CodeLocation mark() {
        return new CodeLocation(currentOffset, currentFrame);
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
        currentFrame = null;
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

    /**
     * overwrites a 16 bit value in the already written bytecode data
     *
     */
    public void overwriteShort(byte[] bytecode, int offset, int value) {
        bytecode[offset] = (byte) (value >> 8);
        bytecode[offset + 1] = (byte) (value);
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

    public void assertTypeOnStack(int position, StackEntryType type, String message) {
        if (getStack().size() <= position) {
            throw new InvalidBytecodeException(message + " Stack State: " + getStack().toString());
        }
        int index = getStack().getContents().size() - 1 - position;
        if (type == StackEntryType.DOUBLE || type == StackEntryType.LONG) {
            index -= 1;
        }
        StackEntryType stype = getStack().getContents().get(index).getType();
        if (stype != type) {
            if (!(type == StackEntryType.OBJECT && stype == StackEntryType.NULL)) {
                throw new InvalidBytecodeException(message + " Stack State: " + getStack().toString());
            }
        }
    }

    public void assertTypeOnStack(StackEntryType type, String message) {
        assertTypeOnStack(0, type, message);
    }

    public void assertNotWideOnStack(int position, String message) {
        if (getStack().size() <= position) {
            throw new InvalidBytecodeException(message + " Stack State: " + getStack().toString());
        }
        int index = getStack().getContents().size() - 1 - position;

        StackEntryType stype = getStack().getContents().get(index).getType();
        if (stype == StackEntryType.TOP) {
            throw new InvalidBytecodeException(message + " Stack State: " + getStack().toString());
        }
    }

    public void assertNotWideOnStack(String message) {
        assertNotWideOnStack(0, message);
    }

    /**
     * Merge the stack frames.
     * <p>
     * If the frames are incompatible then an {@link InvalidBytecodeException} is thrown. If the frames cannot be properly
     * merged then the stack map is marked as invalid
     *
     */
    private void mergeStackFrames(StackFrame stackFrame) {
        if (currentFrame == null) {
            currentFrame = stackFrame;
            stackFrames.put(currentOffset, currentFrame);
            return;
        }
        StackState currentStackState = getStack();
        StackState mergeStackState = stackFrame.getStackState();
        if (currentStackState.size() != mergeStackState.size()) {
            throw new InvalidBytecodeException("Cannot merge stack frames, different stack sizes");
        }
        for (int i = 0; i < mergeStackState.size(); ++i) {
            StackEntry currentEntry = currentStackState.getContents().get(i);
            StackEntry mergeEntry = mergeStackState.getContents().get(i);
            if (mergeEntry.getType() == currentEntry.getType()) {
                if (mergeEntry.getType() == StackEntryType.OBJECT) {
                    if (!mergeEntry.getDescriptor().equals(currentEntry.getDescriptor())) {
                        if (!mergeEntry.equals("Ljava/lang/Object;")) {
                            // we cannot reliably determine if closes common superclass at this point
                            // so we will just mark the stack map attribute as invalid
                            stackMapAttributeValid = false;
                        }
                    }
                }
            } else if (!((mergeEntry.getType() == StackEntryType.NULL && currentEntry.getType() == StackEntryType.OBJECT) || (mergeEntry
                    .getType() == StackEntryType.OBJECT && currentEntry.getType() == StackEntryType.NULL))) {
                throw new InvalidBytecodeException("Cannot merge stack frame " + currentStackState + " with frame "
                        + mergeStackState + " stack entry " + i + " is invalid");
            }
        }

        LocalVariableState currentLocalVariableState = getLocalVars();
        LocalVariableState mergeLocalVariableState = getLocalVars();
        if (currentLocalVariableState.size() < mergeLocalVariableState.size()) {
            throw new InvalidBytecodeException(
                    "Cannot merge stack frames, merge location has less locals than current location");
        }
        for (int i = 0; i < mergeLocalVariableState.size(); ++i) {
            StackEntry currentEntry = currentLocalVariableState.getContents().get(i);
            StackEntry mergeEntry = mergeLocalVariableState.getContents().get(i);
            if (mergeEntry.getType() == currentEntry.getType()) {
                if (mergeEntry.getType() == StackEntryType.OBJECT) {
                    if (!mergeEntry.getDescriptor().equals(currentEntry.getDescriptor())) {
                        if (!mergeEntry.equals("Ljava/lang/Object;")) {
                            // we cannot reliably determine if closes common superclass at this point
                            // so we will just mark the stack map attribute as invalid
                            stackMapAttributeValid = false;
                        }
                    }
                }
            } else if (!((mergeEntry.getType() == StackEntryType.NULL && currentEntry.getType() == StackEntryType.OBJECT) || (mergeEntry
                    .getType() == StackEntryType.OBJECT && currentEntry.getType() == StackEntryType.NULL))) {
                throw new InvalidBytecodeException("Cannot merge stack frame " + currentLocalVariableState + " with frame "
                        + currentLocalVariableState + " local variable entry " + i + " is invalid");
            }
        }
    }

}
