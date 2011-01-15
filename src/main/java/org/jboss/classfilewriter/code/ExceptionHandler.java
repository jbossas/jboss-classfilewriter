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
