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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stuartdouglas.classfilewriter.ClassMethod;

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

    /**
     * construct the initial local variable state for a method
     */
    public LocalVariableState(ClassMethod method) {
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

    private LocalVariableState(final List<StackEntry> contents) {
        this.contents = contents;
    }

    public List<StackEntry> getContents() {
        return Collections.unmodifiableList(contents);
    }

    @Override
    public String toString() {
        return "Local Variables: " + contents.toString();
    }
}
