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

import java.io.DataOutputStream;
import java.io.IOException;

import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;
import org.jboss.classfilewriter.util.DescriptorUtils;

/**
 * A class annotation value
 *
 * @author Stuart Douglas
 *
 */
public class ClassAnnotationValue extends AnnotationValue {

    private final int valueIndex;

    public ClassAnnotationValue(ConstPool constPool, String name, Class<?> value) {
        super(constPool, name);
        this.valueIndex = constPool.addUtf8Entry(DescriptorUtils.makeDescriptor(value));
    }

    public ClassAnnotationValue(ConstPool constPool, String name, String descriptor) {
        super(constPool, name);
        this.valueIndex = constPool.addUtf8Entry(descriptor);
    }

    @Override
    public char getTag() {
        return 'c';
    }

    @Override
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {
        stream.writeShort(valueIndex);
    }

}
