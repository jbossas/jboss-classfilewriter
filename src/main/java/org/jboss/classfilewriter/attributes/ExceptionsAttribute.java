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
package org.jboss.classfilewriter.attributes;

import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The exceptions attribute, stores the checked exceptions a method is declared to throw
 *
 * @author Stuart Douglas
 *
 */
public class ExceptionsAttribute extends Attribute {

    public static final String NAME = "Exceptions";

    private final List<String> exceptionClasses = new ArrayList<String>();

    private final List<Short> exceptionClassIndexes = new ArrayList<Short>();

    private final ConstPool constPool;

    public ExceptionsAttribute(ConstPool constPool) {
        super(NAME, constPool);
        this.constPool = constPool;
    }

    public void addExceptionClass(String exception) {
        exceptionClasses.add(exception);
        exceptionClassIndexes.add(constPool.addClassEntry(exception));
    }

    @Override
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {
        stream.writeInt(2 + exceptionClassIndexes.size() * 2);
        stream.writeShort(exceptionClassIndexes.size());
        for (short i : exceptionClassIndexes) {
            stream.writeShort(i);
        }
    }

}
