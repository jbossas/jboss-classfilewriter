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
package org.jboss.classfilewriter.annotations;

import java.io.IOException;

import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

/**
 * A string annotation value
 */
public class StringAnnotationValue extends AnnotationValue {

    private final int valueIndex;

    private final String value;

    public StringAnnotationValue(ConstPool constPool, String name, String value) {
        super(constPool, name);
        this.value = value;
        this.valueIndex = constPool.addUtf8Entry(value);
    }

    @Override
    public char getTag() {
        return 's';
    }

    @Override
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {
        stream.writeShort(valueIndex);
    }

    public String getValue() {
        return value;
    }

}
