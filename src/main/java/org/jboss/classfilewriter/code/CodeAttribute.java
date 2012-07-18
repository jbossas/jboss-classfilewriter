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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
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
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;
import org.jboss.classfilewriter.util.DescriptorUtils;
import org.jboss.classfilewriter.util.LazySize;

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

    /**
     * maps bytecode offsets to jump locations. As these jump locations where not known when the instruction was written they
     * need to be overwritten when the final bytecode is written out
     * <p/>
     * These jump locations are 32 bit offsets,
     */
    private final Map<Integer, Integer> jumpLocations32 = new HashMap<Integer, Integer>();

    private StackFrame currentFrame;

    private int currentOffset;

    private final List<Attribute> attributes = new ArrayList<Attribute>();

    //disable for now, as this has some issues
    private boolean stackMapAttributeValid = false;

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
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {

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
        for (Entry<Integer, Integer> e : jumpLocations32.entrySet()) {
            overwriteInt(bytecode, e.getKey(), e.getValue());
        }

        LazySize size = stream.writeSize();
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
        for (Attribute attribute : attributes) {
            attribute.write(stream);
        }
        size.markEnd();
        ;
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
        assertTypeOnStack(1, StackEntryType.INT, "aastore requires an int on position 2 stack");
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
        if (locals.size() <= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.OBJECT && entry.getType() != StackEntryType.NULL
                && entry.getType() != StackEntryType.UNINITIALIZED_THIS
                && entry.getType() != StackEntryType.UNITITIALIZED_OBJECT) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables "
                    + locals.toString());
        }

        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.ALOAD);
            writeShort(no);
            currentOffset += 4;
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
        final int jump = currentOffset - end.getOffsetLocation();
        if (end.isJump32Bit()) {
            jumpLocations32.put(end.getBranchLocation(), jump);
        } else {
            if(jump > Short.MAX_VALUE) {
                throw new RuntimeException(jump + " is to big to be written as a 16 bit value");
            }
            jumpLocations.put(end.getBranchLocation(), jump);
        }
    }

    /**
     * Do not use Descriptor format (e.g. Ljava/lang/Object;), the correct form is just java/lang/Object or java.lang.Object
     */
    public void checkcast(String className) {
        assertTypeOnStack(StackEntryType.OBJECT, "checkcast requires reference type on stack");
        int classIndex = constPool.addClassEntry(className);
        writeByte(Opcode.CHECKCAST);
        writeShort(classIndex);
        currentOffset += 3;
        advanceFrame(currentFrame.replace(className));
    }

    public void checkcast(Class<?> clazz) {
        checkcast(clazz.getName());
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
     * <p/>
     * note, if the value is not 0 or 1 then ldc is used instead
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
    public ExceptionHandler exceptionBlockStart(String exceptionType) {
        ExceptionHandler handler = new ExceptionHandler(currentOffset, constPool.addClassEntry(exceptionType), exceptionType,
                currentFrame);
        return handler;
    }

    /**
     * Mark the end of an exception handler block. The last instruction that was written will be the last instruction covered by
     * the handler
     */
    public void exceptionBlockEnd(ExceptionHandler handler) {
        handler.setEnd(currentOffset);
    }

    /**
     * Marks the current code location as the exception handler and adds the handler to the exception handler table;
     */
    public void exceptionHandlerStart(ExceptionHandler handler) {
        if (handler.getEnd() == 0) {
            throw new InvalidBytecodeException(
                    "handler end location must be initialised via exceptionHandlerEnd before calling exceptionHandlerAdd");
        }
        handler.setHandler(currentOffset);
        exceptionTable.add(handler);
        mergeStackFrames(new StackFrame(new StackState(handler.getExceptionType(), constPool), handler.getFrame()
                .getLocalVariableState(), StackFrameType.FULL_FRAME));
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
     * <p/>
     * note, if the value is not 0, 1, 2 then ldc is used instead
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

    public void getfield(String className, String field, Class<?> fieldType) {
        getfield(className, field, DescriptorUtils.makeDescriptor(fieldType));
    }

    public void getfield(String className, String field, String descriptor) {
        assertTypeOnStack(StackEntryType.OBJECT, "getfield requires object on stack");
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.GETFIELD);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.replace(descriptor));
    }

    public void getstatic(String className, String field, Class<?> fieldType) {
        getstatic(className, field, DescriptorUtils.makeDescriptor(fieldType));
    }

    public void getstatic(String className, String field, String descriptor) {
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.GETSTATIC);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.push(descriptor));
    }

    /**
     * writes a goto instruction.
     * <p/>
     * TODO: implemented goto_w
     */
    public void gotoInstruction(CodeLocation location) {
        writeByte(Opcode.GOTO);
        writeShort(location.getLocation() - currentOffset);
        mergeStackFrames(location.getStackFrame());
        currentOffset += 3;
        currentFrame = null;
    }

    /**
     * writes a goto instruction.
     * <p/>
     * TODO: implemented goto_w
     */
    public BranchEnd gotoInstruction() {
        writeByte(Opcode.GOTO);
        writeShort(0);
        currentOffset += 3;
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        currentFrame = null;
        return ret;
    }

    public void i2b() {
        assertTypeOnStack(StackEntryType.INT, "i2b requires int on stack");
        writeByte(Opcode.I2B);
        currentOffset++;
        advanceFrame(currentFrame.replace("B"));
    }

    public void i2c() {
        assertTypeOnStack(StackEntryType.INT, "i2c requires int on stack");
        writeByte(Opcode.I2C);
        currentOffset++;
        advanceFrame(currentFrame.replace("C"));
    }

    public void i2d() {
        assertTypeOnStack(StackEntryType.INT, "i2d requires int on stack");
        writeByte(Opcode.I2D);
        currentOffset++;
        advanceFrame(currentFrame.replace("D"));
    }

    public void i2f() {
        assertTypeOnStack(StackEntryType.INT, "i2f requires int on stack");
        writeByte(Opcode.I2F);
        currentOffset++;
        advanceFrame(currentFrame.replace("F"));
    }

    public void i2l() {
        assertTypeOnStack(StackEntryType.INT, "i2l requires int on stack");
        writeByte(Opcode.I2L);
        currentOffset++;
        advanceFrame(currentFrame.replace("J"));
    }

    public void i2s() {
        assertTypeOnStack(StackEntryType.INT, "i2s requires int on stack");
        writeByte(Opcode.I2S);
        currentOffset++;
        advanceFrame(currentFrame.replace("S"));
    }

    public void iadd() {
        assertTypeOnStack(StackEntryType.INT, "iadd requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "iadd requires int on stack");
        writeByte(Opcode.IADD);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void iaload() {
        assertTypeOnStack(StackEntryType.INT, "iaload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "iaload requires an array in position 2 on the stack");
        writeByte(Opcode.IALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void iand() {
        assertTypeOnStack(StackEntryType.INT, "iand requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "iand requires int on stack");
        writeByte(Opcode.IAND);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void iastore() {
        assertTypeOnStack(StackEntryType.INT, "iastore requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.INT, "iastore requires an int in position 2 on the stack");
        assertTypeOnStack(2, StackEntryType.OBJECT, "iastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.IASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    /**
     * Adds the appropriate iconst instruction.
     * <p/>
     * note, if the value is not in the range -1 to 5 ldc is written instead
     *
     * @param value
     */
    public void iconst(int value) {
        if (value < -1 || value > 5) {
            if(value < -128 || value > 127) {
                ldc(value);
            } else {
                writeByte(Opcode.BIPUSH);
                writeByte(value);
                currentOffset+=2;
                advanceFrame(currentFrame.push("I"));
            }
            return;
        }
        writeByte(Opcode.ICONST_0 + value);
        currentOffset++;
        advanceFrame(currentFrame.push("I"));
    }

    public void idiv() {
        assertTypeOnStack(StackEntryType.INT, "idiv requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "idiv requires int in position 2 on stack");
        writeByte(Opcode.IDIV);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void ifAcmpeq(CodeLocation location) {
        assertTypeOnStack(StackEntryType.OBJECT, "ifAcmpeq requires reference type on stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "ifAcmpeq requires reference type in position 2 on stack");
        writeByte(Opcode.IF_ACMPEQ);
        writeShort(location.getLocation() - currentOffset);
        mergeStackFrames(location.getStackFrame());
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
    }

    public BranchEnd ifAcmpeq() {
        assertTypeOnStack(StackEntryType.OBJECT, "ifAcmpeq requires reference type on stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "ifAcmpeq requires reference type int position 2 on stack");
        writeByte(Opcode.IF_ACMPEQ);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        return ret;
    }

    public void ifAcmpne(CodeLocation location) {
        assertTypeOnStack(StackEntryType.OBJECT, "ifAcmpne requires reference type on stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "ifAcmpne requires reference type in position 2 on stack");
        writeByte(Opcode.IF_ACMPNE);
        writeShort(location.getLocation() - currentOffset);
        mergeStackFrames(location.getStackFrame());
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
    }

    public BranchEnd ifAcmpne() {
        assertTypeOnStack(StackEntryType.OBJECT, "ifAcmpne requires reference type on stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "ifAcmpne requires reference type int position 2 on stack");
        writeByte(Opcode.IF_ACMPNE);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        return ret;
    }

    public void ifIcmpeq(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPEQ, "ifIcmpeq");
    }

    public BranchEnd ifIcmpeq() {
        return addIfIcmp(Opcode.IF_ICMPEQ, "ifIcmpeq");
    }

    public void ifIcmpne(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPNE, "ifIcmpne");
    }

    public BranchEnd ifIcmpne() {
        return addIfIcmp(Opcode.IF_ICMPNE, "ifIcmpne");
    }

    public void ifIcmplt(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPLT, "ifIcmplt");
    }

    public BranchEnd ifIcmplt() {
        return addIfIcmp(Opcode.IF_ICMPLT, "ifIcmplt");
    }

    public void ifIcmple(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPLE, "ifIcmple");
    }

    public BranchEnd ifIcmple() {
        return addIfIcmp(Opcode.IF_ICMPLE, "ifIcmple");
    }

    public void ifIcmpgt(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPGT, "ifIcmpgt");
    }

    public BranchEnd ifIcmpgt() {
        return addIfIcmp(Opcode.IF_ICMPGT, "ifIcmpgt");
    }

    public void ifIcmpge(CodeLocation location) {
        addIfIcmp(location, Opcode.IF_ICMPGE, "ifIcmpge");
    }

    public BranchEnd ifIcmpge() {
        return addIfIcmp(Opcode.IF_ICMPGE, "ifIcmpge");
    }

    public void ifEq(CodeLocation location) {
        addIf(location, Opcode.IFEQ, "ifeq");
    }

    public BranchEnd ifeq() {
        return addIf(Opcode.IFEQ, "ifeq");
    }

    public void ifne(CodeLocation location) {
        addIf(location, Opcode.IFNE, "ifne");
    }

    public BranchEnd ifne() {
        return addIf(Opcode.IFNE, "ifne");
    }

    public void iflt(CodeLocation location) {
        addIf(location, Opcode.IFLT, "iflt");
    }

    public BranchEnd iflt() {
        return addIf(Opcode.IFLT, "iflt");
    }

    public void ifle(CodeLocation location) {
        addIf(location, Opcode.IFLE, "ifle");
    }

    public BranchEnd ifle() {
        return addIf(Opcode.IFLE, "ifle");
    }

    public void ifgt(CodeLocation location) {
        addIf(location, Opcode.IFGT, "ifgt");
    }

    public BranchEnd ifgt() {
        return addIf(Opcode.IFGT, "ifgt");
    }

    public void ifge(CodeLocation location) {
        addIf(location, Opcode.IFGE, "ifge");
    }

    public BranchEnd ifge() {
        return addIf(Opcode.IFGE, "ifge");
    }

    public void ifnotnull(CodeLocation location) {
        addNullComparison(location, Opcode.IFNONNULL, "ifnotnull");
    }

    public BranchEnd ifnotnull() {
        return addNullComparison(Opcode.IFNONNULL, "ifnotnull");
    }

    /**
     * Jump to the given location if the reference type on the top of the stack is null
     */
    public void ifnull(CodeLocation location) {
        addNullComparison(location, Opcode.IFNULL, "ifnull");
    }

    /**
     * Jump to the given location if the reference type on the top of the stack is null.
     * <p/>
     * The {@link BranchEnd} returned from this method is used to set the end point to a future point in the bytecode stream
     */
    public BranchEnd ifnull() {
        return addNullComparison(Opcode.IFNULL, "ifnull");
    }

    public void iinc(int local, int amount) {
        if (getLocalVars().get(local).getType() != StackEntryType.INT) {
            throw new InvalidBytecodeException("iinc requires int at local variable position " + local + " "
                    + getLocalVars().toString());
        }
        if (local > 0xFF || amount > 0xFF) {
            writeByte(Opcode.WIDE);
            writeByte(Opcode.IINC);
            writeShort(local);
            writeShort(amount);
            currentOffset += 6;
        } else {
            writeByte(Opcode.IINC);
            writeByte(local);
            writeByte(amount);
            currentOffset += 3;
        }
        duplicateFrame();
    }

    public void iload(int no) {
        LocalVariableState locals = getLocalVars();
        if (locals.size() <= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.INT) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables "
                    + locals.toString());
        }

        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.ILOAD);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.ILOAD_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.ILOAD);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.push(entry));
    }

    public void imul() {
        assertTypeOnStack(StackEntryType.INT, "imul requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "imul requires int in position 2 on stack");
        writeByte(Opcode.IMUL);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void ineg() {
        assertTypeOnStack(StackEntryType.INT, "ineg requires int on stack");
        writeByte(Opcode.INEG);
        currentOffset++;
        duplicateFrame();
    }

    public void instanceofInstruction(String className) {
        assertTypeOnStack(StackEntryType.OBJECT, "instanceof requires an object reference on the stack");
        int classIndex = constPool.addClassEntry(className);
        writeByte(Opcode.INSTANCEOF);
        writeShort(classIndex);
        currentOffset += 3;
        advanceFrame(currentFrame.replace("I"));
    }

    public void invokespecial(String className, String methodName, String descriptor) {
        String[] params = DescriptorUtils.parameterDescriptors(descriptor);
        String returnType = DescriptorUtils.returnType(descriptor);
        invokespecial(className, methodName, descriptor, returnType, params);
    }

    public void invokespecial(String className, String methodName, String returnType, String[] parameterTypes) {
        String descriptor = DescriptorUtils.methodDescriptor(parameterTypes, returnType);
        invokespecial(className, methodName, descriptor, returnType, parameterTypes);
    }

    public void invokespecial(Constructor<?> constructor) {
        invokespecial(constructor.getDeclaringClass().getName(), "<init>", DescriptorUtils
                .makeDescriptor(constructor), "V", DescriptorUtils.parameterDescriptors(constructor.getParameterTypes()));
    }

    public void invokespecial(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokespacial to invoke a static method");
        }
        invokespecial(method.getDeclaringClass().getName(), method.getName(), DescriptorUtils.methodDescriptor(method),
                DescriptorUtils.makeDescriptor(method.getReturnType()), DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
    }

    private void invokespecial(String className, String methodName, String descriptor, String returnType,
                               String[] parameterTypes) {
        // TODO: validate stack
        int method = constPool.addMethodEntry(className, methodName, descriptor);
        writeByte(Opcode.INVOKESPECIAL);
        writeShort(method);
        currentOffset += 3;
        int pop = 1 + parameterTypes.length;
        for (String argument : parameterTypes) {
            if (argument.equals("D") || argument.equals("J")) {
                pop++;
            }
        }
        if (methodName.equals("<init>")) {
            advanceFrame(currentFrame.constructorCall(pop - 1));
        } else if (returnType.equals("V")) {
            advanceFrame(currentFrame.pop(pop));
        } else {
            advanceFrame(currentFrame.pop(pop).push(returnType));
        }
    }

    public void invokestatic(String className, String methodName, String descriptor) {
        String[] params = DescriptorUtils.parameterDescriptors(descriptor);
        String returnType = DescriptorUtils.returnType(descriptor);
        invokestatic(className, methodName, descriptor, returnType, params);
    }

    public void invokestatic(String className, String methodName, String returnType, String[] parameterTypes) {
        String descriptor = DescriptorUtils.methodDescriptor(parameterTypes, returnType);
        invokestatic(className, methodName, descriptor, returnType, parameterTypes);
    }

    public void invokestatic(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokestatic to invoke a non static method");
        }
        invokestatic(method.getDeclaringClass().getName(), method.getName(), DescriptorUtils.methodDescriptor(method),
                DescriptorUtils.makeDescriptor(method.getReturnType()), DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
    }

    private void invokestatic(String className, String methodName, String descriptor, String returnType, String[] parameterTypes) {
        // TODO: validate stack
        int method = constPool.addMethodEntry(className, methodName, descriptor);
        writeByte(Opcode.INVOKESTATIC);
        writeShort(method);
        currentOffset += 3;
        int pop = parameterTypes.length;
        for (String argument : parameterTypes) {
            if (argument.equals("D") || argument.equals("J")) {
                pop++;
            }
        }
        if (returnType.equals("V")) {
            advanceFrame(currentFrame.pop(pop));
        } else {
            advanceFrame(currentFrame.pop(pop).push(returnType));
        }
    }

    public void invokevirtual(String className, String methodName, String descriptor) {
        String[] params = DescriptorUtils.parameterDescriptors(descriptor);
        String returnType = DescriptorUtils.returnType(descriptor);
        invokevirtual(className, methodName, descriptor, returnType, params);
    }

    public void invokevirtual(String className, String methodName, String returnType, String[] parameterTypes) {
        String descriptor = DescriptorUtils.methodDescriptor(parameterTypes, returnType);
        invokevirtual(className, methodName, descriptor, returnType, parameterTypes);
    }

    public void invokevirtual(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokevirtual to invoke a static method");
        } else if (Modifier.isPrivate(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokevirtual to invoke a private method");
        } else if (method.getDeclaringClass().isInterface()) {
            throw new InvalidBytecodeException("Cannot use invokevirtual to invoke an interface method");
        }
        invokevirtual(method.getDeclaringClass().getName(), method.getName(), DescriptorUtils.methodDescriptor(method),
                DescriptorUtils.makeDescriptor(method.getReturnType()), DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
    }

    private void invokevirtual(String className, String methodName, String descriptor, String returnType,
                               String[] parameterTypes) {
        // TODO: validate stack
        int method = constPool.addMethodEntry(className, methodName, descriptor);
        writeByte(Opcode.INVOKEVIRTUAL);
        writeShort(method);
        currentOffset += 3;
        int pop = 1 + parameterTypes.length;
        for (String argument : parameterTypes) {
            if (argument.equals("D") || argument.equals("J")) {
                pop++;
            }
        }
        if (returnType.equals("V")) {
            advanceFrame(currentFrame.pop(pop));
        } else {
            advanceFrame(currentFrame.pop(pop).push(returnType));
        }
    }

    public void invokeinterface(String className, String methodName, String descriptor) {
        String[] params = DescriptorUtils.parameterDescriptors(descriptor);
        String returnType = DescriptorUtils.returnType(descriptor);
        invokeinterface(className, methodName, descriptor, returnType, params);
    }

    public void invokeinterface(String className, String methodName, String returnType, String[] parameterTypes) {
        String descriptor = DescriptorUtils.methodDescriptor(parameterTypes, returnType);
        invokeinterface(className, methodName, descriptor, returnType, parameterTypes);
    }

    public void invokeinterface(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokeinterface to invoke a static method");
        } else if (Modifier.isPrivate(method.getModifiers())) {
            throw new InvalidBytecodeException("Cannot use invokeinterface to invoke a private method");
        } else if (!method.getDeclaringClass().isInterface()) {
            throw new InvalidBytecodeException("Cannot use invokeinterface to invoke a non interface method");
        }
        invokeinterface(method.getDeclaringClass().getName(), method.getName(), DescriptorUtils.methodDescriptor(method),
                DescriptorUtils.makeDescriptor(method.getReturnType()), DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
    }

    private void invokeinterface(String className, String methodName, String descriptor, String returnType,
                                 String[] parameterTypes) {
        // TODO: validate stack

        int pop = 1 + parameterTypes.length;
        for (String argument : parameterTypes) {
            if (argument.equals("D") || argument.equals("J")) {
                pop++;
            }
        }
        int method = constPool.addInterfaceMethodEntry(className, methodName, descriptor);
        writeByte(Opcode.INVOKEINTERFACE);
        writeShort(method);
        writeByte(pop);
        writeByte(0);
        currentOffset += 5;

        if (returnType.equals("V")) {
            advanceFrame(currentFrame.pop(pop));
        } else {
            advanceFrame(currentFrame.pop(pop).push(returnType));
        }
    }

    public void ior() {
        assertTypeOnStack(StackEntryType.INT, "ior requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "ior requires int on stack");
        writeByte(Opcode.IOR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void irem() {
        assertTypeOnStack(StackEntryType.INT, "irem requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "irem requires int on stack");
        writeByte(Opcode.IREM);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void ishl() {
        assertTypeOnStack(StackEntryType.INT, "ishl requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "ishl requires int on stack");
        writeByte(Opcode.ISHL);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void ishr() {
        assertTypeOnStack(StackEntryType.INT, "ishr requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "ishr requires int on stack");
        writeByte(Opcode.ISHR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void istore(int no) {
        assertTypeOnStack(StackEntryType.INT, "istore requires int on stack");
        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.ISTORE);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.ISTORE_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.ISTORE);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.store(no));
    }

    public void isub() {
        assertTypeOnStack(StackEntryType.INT, "isub requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "isub requires int on stack");
        writeByte(Opcode.ISUB);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void iushr() {
        assertTypeOnStack(StackEntryType.INT, "iushr requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "iushr requires int on stack");
        writeByte(Opcode.IUSHR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void ixor() {
        assertTypeOnStack(StackEntryType.INT, "ixor requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, "ixor requires int on stack");
        writeByte(Opcode.IXOR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void l2d() {
        assertTypeOnStack(StackEntryType.LONG, "l2d requires long on stack");
        writeByte(Opcode.L2D);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("D"));
    }

    public void l2f() {
        assertTypeOnStack(StackEntryType.LONG, "l2f requires long on stack");
        writeByte(Opcode.L2F);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("F"));
    }

    public void l2i() {
        assertTypeOnStack(StackEntryType.LONG, "l2i requires long on stack");
        writeByte(Opcode.L2I);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void ladd() {
        assertTypeOnStack(StackEntryType.LONG, "ladd requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "ladd requires long on stack");
        writeByte(Opcode.LADD);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void laload() {
        assertTypeOnStack(StackEntryType.INT, "laload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "laload requires an array in position 2 on the stack");
        writeByte(Opcode.LALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("J"));
    }

    public void land() {
        assertTypeOnStack(StackEntryType.LONG, "land requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "land requires long on stack");
        writeByte(Opcode.LAND);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lastore() {
        assertTypeOnStack(StackEntryType.LONG, "lastore requires an long on top of the stack");
        assertTypeOnStack(2, StackEntryType.INT, "lastore requires an int in position 2 on the stack");
        assertTypeOnStack(3, StackEntryType.OBJECT, "lastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.LASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop4());
    }

    public void lcmp() {
        assertTypeOnStack(StackEntryType.LONG, "lcmp requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lcmp requires long on stack");
        writeByte(Opcode.LCMP);
        currentOffset++;
        advanceFrame(currentFrame.pop4push1("I"));
    }

    /**
     * Adds the appropriate lconst instruction.
     * <p/>
     * note, if the value is not 0 or 1 then ldc is used instead
     */
    public void lconst(long value) {
        if (value == 0) {
            writeByte(Opcode.LCONST_0);
        } else if (value == 1) {
            writeByte(Opcode.LCONST_1);
        } else {
            ldc2(value);
            return;
        }
        currentOffset++;
        advanceFrame(currentFrame.push("J"));
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
     */
    public void ldc(float value) {
        int index = constPool.addFloatEntry(value);
        ldcInternal(index);
        advanceFrame(currentFrame.push("F"));
    }

    /**
     * Adds an ldc instruction for a String
     * <p/>
     * To load a class literal using ldc use the @{link #loadType(String)} method.
     */
    public void ldc(String value) {
        int index = constPool.addStringEntry(value);
        ldcInternal(index);
        advanceFrame(currentFrame.push("Ljava/lang/String;"));
    }

    /**
     * Adds an ldc instruction for an int.
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
     */
    public void ldc2(long value) {
        int index = constPool.addLongEntry(value);
        writeByte(Opcode.LDC2_W);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.push("J"));
    }

    public void ldiv() {
        assertTypeOnStack(StackEntryType.LONG, "ldiv requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "ldiv requires long in position 3 on stack");
        writeByte(Opcode.LDIV);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lload(int no) {
        LocalVariableState locals = getLocalVars();
        if (locals.size() <= no) {
            throw new InvalidBytecodeException("Cannot load variable at " + no + ". Local Variables: " + locals.toString());
        }
        StackEntry entry = locals.get(no);
        if (entry.getType() != StackEntryType.LONG) {
            throw new InvalidBytecodeException("Invalid local variable at location " + no + " Local Variables "
                    + locals.toString());
        }

        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.LLOAD);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.LLOAD_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.LLOAD);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.push(entry));
    }

    public void lmul() {
        assertTypeOnStack(StackEntryType.LONG, "lmul requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lmul requires long in position 3 on stack");
        writeByte(Opcode.LMUL);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lneg() {
        assertTypeOnStack(StackEntryType.LONG, "lneg requires long on stack");
        writeByte(Opcode.LNEG);
        currentOffset++;
        duplicateFrame();
    }

    /**
     * Generates the apprpriate load instruction for the given type
     *
     * @param type The type of variable
     * @param no   local variable number
     */
    public void load(Class<?> type, int no) {
        load(DescriptorUtils.makeDescriptor(type), no);
    }

    /**
     * Generates the apprpriate load instruction for the given type
     *
     * @param descriptor The descriptor of the variable
     * @param no         local variable number
     */
    public void load(String descriptor, int no) {
        if (descriptor.length() != 1) {
            aload(no);
        } else {
            char type = descriptor.charAt(0);
            switch (type) {
                case 'F':
                    fload(no);
                    break;
                case 'J':
                    lload(no);
                    break;
                case 'D':
                    dload(no);
                    break;
                case 'I':
                case 'S':
                case 'B':
                case 'C':
                case 'Z':
                    iload(no);
                    break;
                default:
                    throw new InvalidBytecodeException("Could not load primitive type: " + type);
            }
        }
    }

    public void loadClass(String className) {
        int index = constPool.addClassEntry(className);
        ldcInternal(index);
        advanceFrame(currentFrame.push("Ljava/lang/Class;"));
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
                case 'V':
                    getstatic(Void.class.getName(), "TYPE", "Ljava/lang/Class;");
                    break;
                default:
                    throw new InvalidBytecodeException("Unkown primitive type: " + type);
            }
        }
    }

    /**
     * Adds a lookup switch statement
     *
     * @param lookupSwitchBuilder
     * @return
     */
    public void lookupswitch(final LookupSwitchBuilder lookupSwitchBuilder) {
        assertTypeOnStack(StackEntryType.INT, "lookupswitch requires an int on the stack");
        writeByte(Opcode.LOOKUPSWITCH);
        final int startOffset = currentOffset;
        currentOffset++;
        while (currentOffset % 4 != 0) {
            writeByte(0);
            currentOffset++;
        }

        StackFrame frame = currentFrame.pop();

        final List<LookupSwitchBuilder.ValuePair> values = new ArrayList<LookupSwitchBuilder.ValuePair>(lookupSwitchBuilder.getValues());

        if (lookupSwitchBuilder.getDefaultLocation() != null) {
            writeInt(lookupSwitchBuilder.getDefaultLocation().getLocation() - currentOffset);
        } else {
            writeInt(0);
            final BranchEnd ret = new BranchEnd(currentOffset, frame, true, startOffset);
            lookupSwitchBuilder.getDefaultBranchEnd().set(ret);
        }
        writeInt(values.size());
        currentOffset += 8;
        Collections.sort(values);
        for (final LookupSwitchBuilder.ValuePair value : values) {
            writeInt(value.getValue());
            currentOffset += 4;
            if (value.getLocation() != null) {
                writeInt(value.getLocation().getLocation());
                currentOffset += 4;
            } else {
                writeInt(0);
                final BranchEnd ret = new BranchEnd(currentOffset, frame, true, startOffset);
                value.getBranchEnd().set(ret);
                currentOffset += 4;
            }
        }
        currentFrame = null;
    }

    public void lor() {
        assertTypeOnStack(StackEntryType.LONG, "lor requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lor requires long in position 3 on stack");
        writeByte(Opcode.LOR);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lrem() {
        assertTypeOnStack(StackEntryType.LONG, "lrem requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lrem requires long in position 3 on stack");
        writeByte(Opcode.LREM);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lshl() {
        assertTypeOnStack(StackEntryType.INT, "lshl requires int on stack");
        assertTypeOnStack(1, StackEntryType.LONG, "lshl requires long in position 2 on stack");
        writeByte(Opcode.LSHL);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void lshr() {
        assertTypeOnStack(StackEntryType.INT, "lshr requires int on stack");
        assertTypeOnStack(1, StackEntryType.LONG, "lshr requires long in position 2 on stack");
        writeByte(Opcode.LSHR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void lstore(int no) {
        assertTypeOnStack(StackEntryType.LONG, "lstore requires long on stack");
        if (no > 0xFF) {
            // wide version
            writeByte(Opcode.WIDE);
            writeByte(Opcode.LSTORE);
            writeShort(no);
            currentOffset += 4;
        } else if (no >= 0 && no < 4) {
            writeByte(Opcode.LSTORE_0 + no);
            currentOffset++;
        } else {
            writeByte(Opcode.LSTORE);
            writeByte(no);
            currentOffset += 2;
        }
        advanceFrame(currentFrame.store(no));
    }

    public void lsub() {
        assertTypeOnStack(StackEntryType.LONG, "lsub requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lsub requires long in position 3 on stack");
        writeByte(Opcode.LSUB);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    public void lushr() {
        assertTypeOnStack(StackEntryType.INT, "lushr requires int on stack");
        assertTypeOnStack(1, StackEntryType.LONG, "lushr requires long in position 2 on stack");
        writeByte(Opcode.LUSHR);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void lxor() {
        assertTypeOnStack(StackEntryType.LONG, "lxor requires long on stack");
        assertTypeOnStack(2, StackEntryType.LONG, "lxor requires long in position 3 on stack");
        writeByte(Opcode.LXOR);
        currentOffset++;
        advanceFrame(currentFrame.pop2());
    }

    /**
     * Gets the location object for the current location in the bytecode. Jumps to this location will begin executing the next
     * instruction that is written to the bytecode stream
     */
    public CodeLocation mark() {
        return new CodeLocation(currentOffset, currentFrame);
    }

    public void monitorenter() {
        assertTypeOnStack(StackEntryType.OBJECT, "monitorenter requires object reference on stack");
        writeByte(Opcode.MONITORENTER);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void monitorexit() {
        assertTypeOnStack(StackEntryType.OBJECT, "monitorexit requires object reference on stack");
        writeByte(Opcode.MONITOREXIT);
        currentOffset++;
        advanceFrame(currentFrame.pop());
    }

    public void multianewarray(String arrayType, int dimensions) {
        StringBuilder newType = new StringBuilder();
        for (int i = 0; i < dimensions; ++i) {
            assertTypeOnStack(i, StackEntryType.INT, "multianewarray requires int on stack in position " + i);
            newType.append('[');
        }
        if (!arrayType.startsWith("[")) {
            newType.append('L');
            newType.append(arrayType);
            newType.append(";");
        } else {
            newType.append(arrayType);
        }
        int classIndex = constPool.addClassEntry(newType.toString());
        writeByte(Opcode.MULTIANEWARRAY);
        writeShort(classIndex);
        writeByte(dimensions);
        currentOffset += 4;

        advanceFrame(currentFrame.pop(dimensions).push(newType.toString()));
    }

    public void newInstruction(String classname) {
        int classIndex = constPool.addClassEntry(classname);
        writeByte(Opcode.NEW);
        writeShort(classIndex);
        StackEntry entry = new StackEntry(StackEntryType.UNITITIALIZED_OBJECT, classname, currentOffset);
        currentOffset += 3;
        advanceFrame(currentFrame.push(entry));
    }

    public void newInstruction(Class<?> clazz) {
        newInstruction(clazz.getName());
    }

    /**
     * arrayType must be a {@link Class} object that represents a primitive type
     */
    public void newarray(Class<?> arrayType) {
        assertTypeOnStack(StackEntryType.INT, "newarray requires int on stack");
        int type = 0;
        String desc;
        if (arrayType == boolean.class) {
            type = Opcode.T_BOOLEAN;
            desc = "[Z";
        } else if (arrayType == char.class) {
            type = Opcode.T_CHAR;
            desc = "[C";
        } else if (arrayType == float.class) {
            type = Opcode.T_FLOAT;
            desc = "[F";
        } else if (arrayType == double.class) {
            type = Opcode.T_DOUBLE;
            desc = "[D";
        } else if (arrayType == byte.class) {
            type = Opcode.T_BYTE;
            desc = "[B";
        } else if (arrayType == short.class) {
            type = Opcode.T_SHORT;
            desc = "[S";
        } else if (arrayType == int.class) {
            type = Opcode.T_INT;
            desc = "[I";
        } else if (arrayType == long.class) {
            type = Opcode.T_LONG;
            desc = "[J";
        } else {
            throw new InvalidBytecodeException("Class " + arrayType + " is not a primitive type");
        }
        writeByte(Opcode.NEWARRAY);
        writeByte(type);
        currentOffset += 2;
        advanceFrame(currentFrame.replace(desc));
    }

    public void nop() {
        writeByte(Opcode.NOP);
        currentOffset++;
        duplicateFrame();
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

    public void putfield(String className, String field, Class<?> fieldType) {
        putfield(className, field, DescriptorUtils.makeDescriptor(fieldType));
    }

    public void putfield(String className, String field, String descriptor) {
        if (!getStack().isOnTop(descriptor)) {
            throw new InvalidBytecodeException("Attempting to put wrong type into  field. Field:" + className + "."
                    + field + " (" + descriptor + "). Stack State: " + getStack().toString());
        }
        if (getStack().top_1().getType() != StackEntryType.UNINITIALIZED_THIS) {
            assertTypeOnStack(1, StackEntryType.OBJECT, "expected object in position 2 on stack");
        }
        int index = constPool.addFieldEntry(className, field, descriptor);
        writeByte(Opcode.PUTFIELD);
        writeShort(index);
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
    }

    public void putstatic(String className, String field, Class<?> fieldType) {
        putstatic(className, field, DescriptorUtils.makeDescriptor(fieldType));
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
     * Adds the appropriate return instruction for the methods return type.
     */
    public void returnInstruction() {
        String returnType = method.getReturnType();
        if (!returnType.equals("V")) {
            if (!getStack().isOnTop(returnType)) {
                throw new InvalidBytecodeException(returnType + " is not on top of stack. " + getStack().toString());
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

    public void saload() {
        assertTypeOnStack(StackEntryType.INT, "saload requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.OBJECT, "saload requires an array in position 2 on the stack");
        writeByte(Opcode.SALOAD);
        currentOffset++;
        advanceFrame(currentFrame.pop2push1("I"));
    }

    public void sastore() {
        assertTypeOnStack(StackEntryType.INT, "sastore requires an int on top of the stack");
        assertTypeOnStack(1, StackEntryType.INT, "sastore requires an int in position 2 on the stack");
        assertTypeOnStack(2, StackEntryType.OBJECT, "sastore requires an array reference in position 3 on the stack");
        writeByte(Opcode.SASTORE);
        currentOffset++;
        advanceFrame(currentFrame.pop3());
    }

    public void sipush(short value) {
        writeByte(Opcode.SIPUSH);
        writeShort(value);
        currentOffset += 3;
        advanceFrame(currentFrame.push("S"));
    }

    public void swap() {
        assertNotWideOnStack("swap cannot be used when wide type is on top of stack");
        assertNotWideOnStack(1, "swap cannot be used when wide type is on position 1 of the stack");
        writeByte(Opcode.SWAP);
        currentOffset++;
        advanceFrame(currentFrame.swap());
    }

    public void tableswitch(final TableSwitchBuilder builder) {
        assertTypeOnStack(StackEntryType.INT, "lookupswitch requires an int on the stack");
        writeByte(Opcode.TABLESWITCH);
        final int startOffset = currentOffset;
        currentOffset++;
        while (currentOffset % 4 != 0) {
            writeByte(0);
            currentOffset++;
        }

        if(builder.getHigh() - builder.getLow() + 1 != builder.getValues().size()) {
            throw new RuntimeException("high - low + 1 != the number of values in the table");
        }

        StackFrame frame = currentFrame.pop();

        if (builder.getDefaultLocation() != null) {
            writeInt(builder.getDefaultLocation().getLocation() - currentOffset);
        } else {
            writeInt(0);
            final BranchEnd ret = new BranchEnd(currentOffset, frame, true, startOffset);
            builder.getDefaultBranchEnd().set(ret);
        }
        writeInt(builder.getLow());
        writeInt(builder.getHigh());
        currentOffset += 12;
        for (final TableSwitchBuilder.ValuePair value : builder.getValues()) {
            if (value.getLocation() != null) {
                writeInt(value.getLocation().getLocation());
                currentOffset += 4;
            } else {
                writeInt(0);
                final BranchEnd ret = new BranchEnd(currentOffset, frame, true, startOffset);
                value.getBranchEnd().set(ret);
                currentOffset += 4;
            }
        }
        currentFrame = null;
    }

    /**
     * loads all parameters onto the stack.
     * <p/>
     * If this method is non-static then the parameter at location 0 (i.e. this object) is not pushed.
     */
    public void loadMethodParameters() {
        int index = method.isStatic() ? 0 : 1;
        for (String type : method.getParameters()) {
            if (type.length() > 1) {
                // object or array
                aload(index);
            } else if (type.equals("D")) {
                dload(index);
                index++;
            } else if (type.equals("J")) {
                lload(index);
                index++;
            } else if (type.equals("F")) {
                fload(index);
            } else {
                iload(index);
            }
            index++;
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
            if(n > Short.MAX_VALUE) {
                throw new RuntimeException(n + " is to big to be written as a 16 bit value");
            }
            data.writeShort(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeInt(int n) {
        try {
            data.writeInt(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * overwrites a 16 bit value in the already written bytecode data
     */
    private void overwriteShort(byte[] bytecode, int offset, int value) {
        bytecode[offset] = (byte) (value >> 8);
        bytecode[offset + 1] = (byte) (value);
    }

    /**
     * overwrites a 32 bit value in the already written bytecode data
     */
    private void overwriteInt(byte[] bytecode, int offset, int value) {
        bytecode[offset] = (byte) (value >> 24);
        bytecode[offset + 1] = (byte) (value >> 16);
        bytecode[offset + 2] = (byte) (value >> 8);
        bytecode[offset + 3] = (byte) (value);
    }

    public LinkedHashMap<Integer, StackFrame> getStackFrames() {
        return new LinkedHashMap<Integer, StackFrame>(stackFrames);
    }

    public void setupFrame(String ... types) {
        final LocalVariableState localVariableState = new LocalVariableState(constPool, types);
        final StackFrame f = new StackFrame(new StackState(constPool), localVariableState, StackFrameType.FULL_FRAME);
        mergeStackFrames(f);
    }

    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Adds a duplicate of the current frame to the current position.
     * <p/>
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
        if(currentFrame == null) {
            throw new RuntimeException("No local variable information available, call setupFrame first");
        }
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
     * <p/>
     * If the frames are incompatible then an {@link InvalidBytecodeException} is thrown. If the frames cannot be properly
     * merged then the stack map is marked as invalid
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

    private void addIfIcmp(CodeLocation location, int opcode, String name) {
        assertTypeOnStack(StackEntryType.INT, name + " requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, name + " requires int in position 2 on stack");
        writeByte(opcode);
        writeShort(location.getLocation() - currentOffset);
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
        mergeStackFrames(location.getStackFrame());
    }

    private BranchEnd addIfIcmp(int opcode, String name) {
        assertTypeOnStack(StackEntryType.INT, name + " requires int on stack");
        assertTypeOnStack(1, StackEntryType.INT, name + " requires int int position 2 on stack");
        writeByte(opcode);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop2());
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        return ret;
    }

    private void addIf(CodeLocation location, int opcode, String name) {
        assertTypeOnStack(StackEntryType.INT, name + " requires int on stack");
        writeByte(opcode);
        writeShort(location.getLocation() - currentOffset);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
        mergeStackFrames(location.getStackFrame());
    }

    private BranchEnd addIf(int opcode, String name) {
        assertTypeOnStack(StackEntryType.INT, name + " requires int on stack");
        writeByte(opcode);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        return ret;
    }

    private void addNullComparison(CodeLocation location, int opcode, String name) {
        assertTypeOnStack(StackEntryType.OBJECT, name + " requires reference type on stack");
        writeByte(opcode);
        writeShort(location.getLocation() - currentOffset);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
        mergeStackFrames(location.getStackFrame());
    }

    private BranchEnd addNullComparison(int opcode, String name) {
        assertTypeOnStack(StackEntryType.OBJECT, name + " requires reference type on stack");
        writeByte(opcode);
        writeShort(0);
        currentOffset += 3;
        advanceFrame(currentFrame.pop());
        BranchEnd ret = new BranchEnd(currentOffset - 2, currentFrame, currentOffset - 3);
        return ret;
    }

}
