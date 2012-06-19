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

/**
 * Represents a location in the bytecode.
 *
 * @author Stuart Douglas
 *
 */
public class CodeLocation {

    /**
     * The absolution location in the bytecode stream. This will always point to a valid jump location
     */
    private final int location;

    /**
     * The stack frame at the given bytecode location
     */
    private final StackFrame stackFrame;

    CodeLocation(int location, StackFrame stackFrame) {
        this.location = location;
        this.stackFrame = stackFrame;
    }

    int getLocation() {
        return location;
    }

    StackFrame getStackFrame() {
        return stackFrame;
    }

}
