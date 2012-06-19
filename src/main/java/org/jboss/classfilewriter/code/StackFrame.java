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

import org.jboss.classfilewriter.ClassMethod;

/**
 * Represents a stack frame in the virtual machine. Holds the state of the local variable array and the stack
 *
 * @author Stuart Douglas
 *
 */
public class StackFrame {

    /**
     * The current state of the stack
     */
    private final StackState stackState;

    /**
     * The local variable state
     */
    private final LocalVariableState localVariableState;

    private final StackFrameType type;

    /**
     * Creates the initial stack frame
     */
    public StackFrame(ClassMethod method) {
        this.stackState = new StackState(method.getClassFile().getConstPool());
        this.localVariableState = new LocalVariableState(method);
        this.type = StackFrameType.FULL_FRAME;
    }

    public StackFrame(StackState stackState, LocalVariableState localVariableState, StackFrameType type) {
        this.stackState = stackState;
        this.localVariableState = localVariableState;
        this.type = type;
    }

    public StackState getStackState() {
        return stackState;
    }

    public LocalVariableState getLocalVariableState() {
        return localVariableState;
    }

    /**
     * push an operand of the given type onto the stack
     * <p>
     * If the entry is wide then a corresponding TOP type will be created
     */
    public StackFrame push(String type) {
        StackState ns = stackState.push(type);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * push an operand of the given type onto the stack.
     * <p>
     * If the entry is wide then a corresponding TOP type will be created
     */
    public StackFrame push(StackEntry entry) {
        StackState ns = stackState.push(entry);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * pushes a null type onto the stack
     *
     * @return
     */
    public StackFrame aconstNull() {
        StackState ns = stackState.aconstNull();
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));

    }

    public StackFrame pop(int no) {
        StackState ns = stackState.pop(no);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * pops an operand from the stack
     */
    public StackFrame pop() {
        StackState ns = stackState.pop(1);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * pops 2 operands from the stack
     */
    public StackFrame pop2() {
        StackState ns = stackState.pop(2);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * pops 3 operands from the stack
     */
    public StackFrame pop3() {
        StackState ns = stackState.pop(3);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * pops 4 operands from the stack
     */
    public StackFrame pop4() {
        StackState ns = stackState.pop(4);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * replace the operand at the top of the stack with the given operand
     */
    public StackFrame replace(String type) {
        // TODO: inefficinet
        StackState ns = stackState.pop(1).push(type);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    public StackFrame dup() {
        StackState ns = stackState.dup();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    public StackFrame dupX1() {
        StackState ns = stackState.dupX1();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    public StackFrame dupX2() {
        StackState ns = stackState.dupX2();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    public StackFrame dup2() {
        StackState ns = stackState.dup2();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    public StackFrame dup2X1() {
        StackState ns = stackState.dup2X1();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    public StackFrame dup2X2() {
        StackState ns = stackState.dup2X2();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    /**
     * Store the variable on top of the stack into a local variable, poping the variable from the stack. Wide types are handled
     * automatically
     */
    public StackFrame store(int no) {
        StackEntry top = stackState.top();
        StackState ns;
        LocalVariableState ls;
        if(top.getType() == StackEntryType.TOP) { //wide type
            StackEntry type = stackState.top_1();
            ns = stackState.pop(2);
            ls = localVariableState.storeWide(no, type);
        } else {
            StackEntry type = stackState.top();
            ns = stackState.pop(1);
            ls = localVariableState.store(no, type);
        }
        return new StackFrame(ns, ls, StackFrameType.FULL_FRAME);
    }

    /**
     * remote the top two operands and replace them with an different operand
     *
     */
    public StackFrame pop2push1(String type) {
        StackState ns = stackState.pop(2).push(type);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    /**
     * remote the top two operands and replace them with an different operand
     *
     */
    public StackFrame pop4push1(String type) {
        StackState ns = stackState.pop(4).push(type);
        return new StackFrame(ns, localVariableState, typeNoLocalChange(ns));
    }

    @Override
    public String toString() {
        return "StackFrame [localVariableState=" + localVariableState + ", stackState=" + stackState + "]";
    }

    /**
     * marks the value in potition initializedValueStackPosition as initialized. This also pops this value and everything above
     * it
     */
    public StackFrame constructorCall(int initializedValueStackPosition) {
        StackEntry entry = stackState.getContents().get(stackState.getContents().size() - 1 - initializedValueStackPosition);
        StackState ns = stackState.constructorCall(initializedValueStackPosition, entry);
        LocalVariableState locals = localVariableState.constructorCall(entry);
        return new StackFrame(ns, locals, StackFrameType.FULL_FRAME);

    }

    public StackFrame swap() {
        StackState ns = stackState.swap();
        return new StackFrame(ns, localVariableState, StackFrameType.FULL_FRAME);
    }

    /**
     * determins the type of stack frame this is when no local variables have changed
     */
    private StackFrameType typeNoLocalChange(StackState ns) {
        int size = ns.getContents().size();
        if (size == 0) {
            return StackFrameType.SAME_FRAME;
        } else if (size == 1) {
            return StackFrameType.SAME_LOCALS_1_STACK;
        }
        return StackFrameType.FULL_FRAME;

    }

    public StackFrameType getType() {
        return type;
    }

}
