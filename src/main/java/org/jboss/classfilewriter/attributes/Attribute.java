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

import org.jboss.classfilewriter.WritableEntry;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

import java.io.IOException;

/**
 * Represents an attribute in a class file
 *
 * @author Stuart Douglas
 *
 */
public abstract class Attribute implements WritableEntry {

    private final String name;
    private final short nameIndex;
    protected final ConstPool constPool;

    public Attribute(String name, final ConstPool constPool) {
        this.name = name;
        this.nameIndex = constPool.addUtf8Entry(name);
        this.constPool = constPool;
    }

    public void write(ByteArrayDataOutputStream stream) throws IOException {
        stream.writeShort(nameIndex);
        writeData(stream);
    }

    public abstract void writeData(ByteArrayDataOutputStream stream) throws IOException;

    public String getName() {
        return name;
    }

}
