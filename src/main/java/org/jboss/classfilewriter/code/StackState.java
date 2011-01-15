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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.classfilewriter.constpool.ConstPool;


/**
 * In immutable stack state, which may be shared between frames
 *
 * @author Stuart Douglas
 *
 */
public class StackState {

    /**
     * The contents of the stack
     * <p>
     * This list may be shared between frames, so it must never be modified
     * <p>
     * The very first element represents the bottom of the stack, with the last element representing the top. Wide elements are
     * stored as Wide, Top, with the {@link StackEntryType#TOP} on the top of the stack
     */
    private final List<StackEntry> contents;

    private final ConstPool constPool;

    public StackState(ConstPool constPool) {
        this.contents = new ArrayList<StackEntry>(0);
        this.constPool = constPool;
    }

    public StackState(String exceptionType, ConstPool constPool) {
        this.contents = new ArrayList<StackEntry>(1);
        this.contents.add(new StackEntry(StackEntryType.OBJECT, "L" + exceptionType + ";", constPool));
        this.constPool = constPool;
    }

    private StackState(final List<StackEntry> contents, ConstPool constPool) {
        this.contents = contents;
        this.constPool = constPool;
    }

    /**
     * checks that the appropriate object type is on top of the stack
     */
    public boolean isOnTop(String descriptor) {
        if (contents.isEmpty()) {
            return false;
        }
        StackEntry entry = StackEntry.of(descriptor, constPool);
        StackEntry top = top();
        if (entry.isWide()) {
            if (contents.size() == 1) {
                return false;
            }
            return top_1().getType() == entry.getType();
        }
        if (top.getType() == StackEntryType.NULL && entry.getType() == StackEntryType.OBJECT) {
            return true;
        }
        return top.getType() == entry.getType();
    }

    public int size() {
        return contents.size();
    }

    /**
     * push a type on to the top of the stack
     */
    public StackState push(String type) {
        StackEntry entry = StackEntry.of(type, constPool);
        if (entry.getType() == StackEntryType.DOUBLE || entry.getType() == StackEntryType.LONG) {
            return newStack(entry, new StackEntry(StackEntryType.TOP, type));
        }
        return newStack(entry);
    }

    /**
     * push a type on to the top of the stack
     */
    public StackState push(StackEntry entry) {
        if (entry.getType() == StackEntryType.DOUBLE || entry.getType() == StackEntryType.LONG) {
            return newStack(entry, new StackEntry(StackEntryType.TOP, entry.getDescriptor()));
        }
        return newStack(entry);
    }

    public StackState aconstNull() {
        StackEntry entry = new StackEntry(StackEntryType.NULL, null);
        return newStack(entry);
    }

    /**
     * pop a non-wide type from the top of the stack
     */
    public StackState pop() {
        if (contents.isEmpty()) {
            throw new InvalidBytecodeException("Cannot pop from empty stack");
        }
        StackEntry type = top();
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot pop wide type");
        }
        return new StackState(contents.subList(0, contents.size() - 1), constPool);
    }

    /**
     * pop a wide type from the top of the stack
     */
    public StackState pop2() {
        if (contents.size() < 2) {
            throw new InvalidBytecodeException("cannot pop2, only " + contents.size() + " on stack " + toString());
        }
        StackEntry type = top_1();
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot pop2 when second type on stack is wide " + toString());
        }
        return new StackState(contents.subList(0, contents.size() - 2), constPool);
    }

    /**
     * removes three items from the top of the stack
     * 
     * @return
     */
    public StackState pop3() {
        if (contents.size() < 3) {
            throw new InvalidBytecodeException("cannot pop3, only " + contents.size() + " on stack " + toString());
        }
        StackEntry type = top_2();
        if (type.isWide()) {
            throw new InvalidBytecodeException("Cannot pop3 when third type on stack is wide " + toString());
        }
        return new StackState(contents.subList(0, contents.size() - 3), constPool);
    }

    public StackState dup() {
        if (contents.isEmpty()) {
            throw new InvalidBytecodeException("cannot dup empty stack");
        }
        StackEntry type = top();
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup wide type");
        }
        return newStack(type);
    }

    public StackState dup2() {
        if (contents.size() <2) {
            throw new InvalidBytecodeException("cannot dup2, stack size is " + contents.size() + " " + toString());
        }
        StackEntry t1 = top();
        StackEntry t2 = top_1();
        if (t2.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup2 when second type on stack is wide: " + toString());
        }
        return newStack(t1, t2);
    }
    private StackState newStack(StackEntry... pushValues) {
        ArrayList<StackEntry> ret = new ArrayList<StackEntry>(pushValues.length + contents.size());
        ret.addAll(contents);
        for (StackEntry s : pushValues) {
            ret.add(s);
        }
        return new StackState(ret, constPool);
    }

    public StackEntry top() {
        return contents.get(contents.size() - 1);
    }

    public StackEntry top_1() {
        return contents.get(contents.size() - 2);
    }

    public StackEntry top_2() {
        return contents.get(contents.size() - 3);
    }

    public StackEntry top_3() {
        return contents.get(contents.size() - 4);
    }
    @Override
    public String toString() {
        return "Stack: " + contents.toString();
    }

    public List<StackEntry> getContents() {
        return Collections.unmodifiableList(contents);
    }

}
