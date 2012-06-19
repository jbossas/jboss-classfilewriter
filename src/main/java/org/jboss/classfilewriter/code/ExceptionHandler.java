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
 *
 * @author Stuart Douglas
 *
 */
public class ExceptionHandler {

    private final int start;
    private final int exceptionIndex;
    private final String exceptionType;
    private final StackFrame frame;
    private int end;
    private int handler;

    ExceptionHandler(int start, int exceptionIndex, String exceptionType, StackFrame frame) {
        this.start = start;
        this.exceptionIndex = exceptionIndex;
        this.exceptionType = exceptionType;
        this.frame = frame;
    }

    int getEnd() {
        return end;
    }

    void setEnd(int end) {
        this.end = end;
    }

    int getHandler() {
        return handler;
    }

    void setHandler(int handler) {
        this.handler = handler;
    }

    int getStart() {
        return start;
    }

    int getExceptionIndex() {
        return exceptionIndex;
    }

    StackFrame getFrame() {
        return frame;
    }

    public String getExceptionType() {
        return exceptionType;
    }
}
