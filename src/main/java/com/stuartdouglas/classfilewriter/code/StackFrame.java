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

import com.stuartdouglas.classfilewriter.ClassMethod;

/**
 * Represents a stack frame in the virtual machine. Holds the state of the local variable array and the stack
 * 
 * @author Stuart Douglas
 * 
 */
public class StackFrame {

    /**
     * The absolute bytecode offset of this frame
     */
    private final int position;

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
        this.position = 0;
        this.stackState = new StackState(method.getClassFile().getConstPool());
        this.localVariableState = new LocalVariableState(method);
    }

    public int getPosition() {
        return position;
    }

    public StackState getStackState() {
        return stackState;
    }

    public LocalVariableState getLocalVariableState() {
        return localVariableState;
    }

}
