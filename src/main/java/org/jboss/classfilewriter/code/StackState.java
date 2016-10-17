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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.DescriptorUtils;


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
        this.contents.add(new StackEntry(StackEntryType.OBJECT, DescriptorUtils.makeDescriptor(exceptionType), constPool));
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
    public StackState pop(int no) {
        if (no == 0) {
            return this;
        }
        if (contents.size() < no) {
            throw new InvalidBytecodeException("cannot pop" + no + ", only " + contents.size() + " on stack " + toString());
        }
        StackEntry type = contents.get(contents.size() - no);
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Pop" + no + " would split wide type " + toString());
        }
        return new StackState(contents.subList(0, contents.size() - no), constPool);
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

    public StackState dupX1() {
        if (contents.size() < 2) {
            throw new InvalidBytecodeException("cannot dup_x1, stack does not have enough items");
        }
        StackEntry type = top();
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup_x1 wide type");
        }
        ArrayList<StackEntry> ret = new ArrayList<StackEntry>(1 + contents.size());
        for (int i = 0; i < contents.size(); ++i) {
            if (i == contents.size() - 2) {
                ret.add(top());
            }
            ret.add(contents.get(i));
        }
        return new StackState(ret, constPool);
    }

    public StackState dupX2() {
        if (contents.size() < 3) {
            throw new InvalidBytecodeException("cannot dup_x1, stack does not have enough items");
        }
        StackEntry type = top();
        if (type.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup_x1 wide type");
        }
        ArrayList<StackEntry> ret = new ArrayList<StackEntry>(1 + contents.size());
        for (int i = 0; i < contents.size(); ++i) {
            if (i == contents.size() - 3) {
                ret.add(top());
            }
            ret.add(contents.get(i));
        }
        return new StackState(ret, constPool);
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
        return newStack(t2, t1);
    }

    public StackState dup2X1() {
        if (contents.size() < 3) {
            throw new InvalidBytecodeException("cannot dup2X1, stack size is " + contents.size() + " " + toString());
        }
        StackEntry t1 = top();
        StackEntry t2 = top_1();
        StackEntry t3 = top_2();
        if (t2.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup2X1 when second type on stack is wide: " + toString());
        }
        if (t3.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup2X2 when third type on stack is wide: " + toString());
        }
        ArrayList<StackEntry> ret = new ArrayList<StackEntry>(2 + contents.size());
        for (int i = 0; i < contents.size(); ++i) {
            if (i == contents.size() - 3) {
                ret.add(t2);
                ret.add(t1);
            }
            ret.add(contents.get(i));
        }
        return new StackState(ret, constPool);
    }

    public StackState dup2X2() {
        if (contents.size() < 4) {
            throw new InvalidBytecodeException("cannot dup2X2, stack size is " + contents.size() + " " + toString());
        }
        StackEntry t1 = top();
        StackEntry t2 = top_1();
        StackEntry t4 = top_3();
        if (t2.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup2X2 when second type on stack is wide: " + toString());
        }
        if (t4.getType() == StackEntryType.TOP) {
            throw new InvalidBytecodeException("Cannot dup2X2 when fourth type on stack is wide: " + toString());
        }
        ArrayList<StackEntry> ret = new ArrayList<StackEntry>(2 + contents.size());
        for (int i = 0; i < contents.size(); ++i) {
            if (i == contents.size() - 4) {
                ret.add(t2);
                ret.add(t1);
            }
            ret.add(contents.get(i));
        }
        return new StackState(ret, constPool);
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

    public StackState constructorCall(int initializedValueStackPosition, StackEntry entry) {
        List<StackEntry> newContents = new ArrayList<StackEntry>(contents.size());
        if (entry.getType() == StackEntryType.UNINITIALIZED_THIS) {
            for (int i = 0; i < contents.size() - 1 - initializedValueStackPosition; ++i) {
                StackEntry stackEntry = contents.get(i);
                if (stackEntry.getType() == StackEntryType.UNINITIALIZED_THIS) {
                    newContents.add(StackEntry.of(stackEntry.getDescriptor(), constPool));
                } else {
                    newContents.add(stackEntry);
                }
            }
            return new StackState(newContents, constPool);
        } else if (entry.getType() == StackEntryType.UNITITIALIZED_OBJECT) {
            for (int i = 0; i < contents.size() - 1 - initializedValueStackPosition; ++i) {
                StackEntry stackEntry = contents.get(i);
                if (stackEntry.getType() == StackEntryType.UNITITIALIZED_OBJECT
                        && stackEntry.getNewInstructionLocation() == entry.getNewInstructionLocation()) {
                    newContents.add(StackEntry.of(stackEntry.getDescriptor(), constPool));
                } else {
                    newContents.add(stackEntry);
                }
            }
            return new StackState(newContents, constPool);
        } else {
            throw new InvalidBytecodeException("Object at position " + initializedValueStackPosition
                    + " is not an unitialized object. " + toString());
        }
    }

    public StackState updateMerged(int pos, StackEntry frame) {
        List<StackEntry> newContents = new ArrayList<StackEntry>(contents);
        newContents.remove(pos);
        newContents.add(pos, frame);
        return new StackState(newContents, constPool);
    }

    public StackState swap() {
        int size = contents.size();
        List<StackEntry> newContents = new ArrayList<StackEntry>(contents.subList(0, size - 2));
        newContents.add(contents.get(size - 1));
        newContents.add(contents.get(size - 2));
        return new StackState(newContents, constPool);
    }

}
