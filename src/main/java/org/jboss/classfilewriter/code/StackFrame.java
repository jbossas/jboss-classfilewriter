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

    /**
     * Creates the initial stack frame
     */
    public StackFrame(ClassMethod method) {
        this.stackState = new StackState(method.getClassFile().getConstPool());
        this.localVariableState = new LocalVariableState(method);
    }

    public StackFrame(StackState stackState, LocalVariableState localVariableState) {
        this.stackState = stackState;
        this.localVariableState = localVariableState;
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
        return new StackFrame(ns, localVariableState);
    }

    /**
     * push an operand of the given type onto the stack.
     * <p>
     * If the entry is wide then a corresponding TOP type will be created
     */
    public StackFrame push(StackEntry entry) {
        StackState ns = stackState.push(entry);
        return new StackFrame(ns, localVariableState);
    }

    /**
     * pushes a null type onto the stack
     *
     * @return
     */
    public StackFrame aconstNull() {
        StackState ns = stackState.aconstNull();
        return new StackFrame(ns, localVariableState);

    }

    /**
     * pops an operand from the stack
     */
    public StackFrame pop() {
        StackState ns = stackState.pop();
        return new StackFrame(ns, localVariableState);
    }

    /**
     * pops 2 operands from the stack
     */
    public StackFrame pop2() {
        StackState ns = stackState.pop2();
        return new StackFrame(ns, localVariableState);
    }

    /**
     * pops 3 operands from the stack
     */
    public StackFrame pop3() {
        StackState ns = stackState.pop3();
        return new StackFrame(ns, localVariableState);
    }

    /**
     * pops 4 operands from the stack
     */
    public StackFrame pop4() {
        StackState ns = stackState.pop4();
        return new StackFrame(ns, localVariableState);
    }

    /**
     * replace the operand at the top of the stack with the given operand
     */
    public StackFrame replace(String type) {
        // TODO: inefficinet
        StackState ns = stackState.pop().push(type);
        return new StackFrame(ns, localVariableState);
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
            ns = stackState.pop2();
            ls = localVariableState.storeWide(no, type);
        } else {
            StackEntry type = stackState.top();
            ns = stackState.pop();
            ls = localVariableState.store(no, type);
        }
        return new StackFrame(ns,ls);
    }

    /**
     * remote the top two operands and replace them with an different operand
     *
     */
    public StackFrame pop2push1(String type) {
        StackState ns = stackState.pop2().push(type);
        return new StackFrame(ns, localVariableState);
    }

    /**
     * remote the top two operands and replace them with an different operand
     * 
     */
    public StackFrame pop4push1(String type) {
        StackState ns = stackState.pop4().push(type);
        return new StackFrame(ns, localVariableState);
    }

    @Override
    public String toString() {
        return "StackFrame [localVariableState=" + localVariableState + ", stackState=" + stackState + "]";
    }

}
