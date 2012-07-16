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

import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * In immutable local variable state
 *
 * @author Stuart Douglas
 *
 */
public class LocalVariableState {

    /**
     * The contents, null is used to represent the additional spot taken up by a wide variable.
     * <p>
     * This list may be shared between frames, so it must never be modified
     * <p>
     * The very first element represents the first local variable (this for non static methods)
     */
    private final List<StackEntry> contents;

    private final ConstPool constPool;

    /**
     * construct the initial local variable state for a method
     */
    public LocalVariableState(ClassMethod method) {
        this.constPool = method.getClassFile().getConstPool();
        contents = new ArrayList<StackEntry>();
        if (!method.isStatic()) {
            if (method.isConstructor()) {
                contents.add(new StackEntry(StackEntryType.UNINITIALIZED_THIS, method.getClassFile().getDescriptor()));
            } else {
                contents.add(StackEntry.of(method.getClassFile().getDescriptor(), method.getClassFile().getConstPool()));
            }
        }
        for (String param : method.getParameters()) {
            StackEntry entry = StackEntry.of(param, method.getClassFile().getConstPool());
            contents.add(entry);
            if (entry.isWide()) {
                contents.add(new StackEntry(StackEntryType.TOP, param));
            }
        }
    }

    public LocalVariableState(final ConstPool pool, final String ... entries) {
        this.constPool = pool;
        contents = new ArrayList<StackEntry>();
        for(String c : entries) {
            final StackEntry entry = StackEntry.of(c, constPool);
            contents.add(entry);
        }
    }

    private LocalVariableState(final List<StackEntry> contents, ConstPool constPool) {
        this.contents = contents;
        this.constPool = constPool;
    }

    public List<StackEntry> getContents() {
        return Collections.unmodifiableList(contents);
    }

    public StackEntry get(int index) {
        return contents.get(index);
    }

    public LocalVariableState storeWide(int index, StackEntry entry) {
        ArrayList<StackEntry> newContents = new ArrayList<StackEntry>(contents.size());
        for (int i = 0; i <= index || i < contents.size(); ++i) {
            if (index == i) {
                newContents.add(entry);
                newContents.add(new StackEntry(StackEntryType.TOP, entry.getDescriptor()));
                ++i;
            } else if (i >= contents.size()) {
                // write a null in unitialised slots
                // not sure if this is correct
                newContents.add(new StackEntry(StackEntryType.NULL, null));
            } else {
                newContents.add(contents.get(i));
            }
        }
        return new LocalVariableState(newContents, constPool);
    }

    public LocalVariableState store(int index, StackEntry entry) {
        ArrayList<StackEntry> newContents = new ArrayList<StackEntry>(contents.size());
        for (int i = 0; i <= index || i < contents.size(); ++i) {
            if (index == i) {
                newContents.add(entry);
            } else if (i >= contents.size()) {
                // write a null in unitialised slots
                // not sure if this is correct
                newContents.add(new StackEntry(StackEntryType.NULL, null));
            } else {
                newContents.add(contents.get(i));
            }
        }
        return new LocalVariableState(newContents, constPool);
    }

    public int size() {
        return contents.size();
    }

    @Override
    public String toString() {
        return "Local Variables: " + contents.toString();
    }

    public LocalVariableState constructorCall(StackEntry entry) {
        List<StackEntry> newContents = new ArrayList<StackEntry>(contents.size());
        if (entry.getType() == StackEntryType.UNINITIALIZED_THIS) {
            for (int i = 0; i < contents.size(); ++i) {
                StackEntry stackEntry = contents.get(i);
                if (stackEntry.getType() == StackEntryType.UNINITIALIZED_THIS) {
                    newContents.add(StackEntry.of(stackEntry.getDescriptor(), constPool));
                } else {
                    newContents.add(stackEntry);
                }
            }
            return new LocalVariableState(newContents, constPool);
        } else if (entry.getType() == StackEntryType.UNITITIALIZED_OBJECT) {
            for (int i = 0; i < contents.size(); ++i) {
                StackEntry stackEntry = contents.get(i);
                if (stackEntry.getType() == StackEntryType.UNITITIALIZED_OBJECT
                        && stackEntry.getNewInstructionLocation() == entry.getNewInstructionLocation()) {
                    newContents.add(StackEntry.of(stackEntry.getDescriptor(), constPool));
                } else {
                    newContents.add(stackEntry);
                }
            }
            return new LocalVariableState(newContents, constPool);
        } else {
            throw new InvalidBytecodeException("entry is not an unitialized object. " + toString());
        }
    }
}
