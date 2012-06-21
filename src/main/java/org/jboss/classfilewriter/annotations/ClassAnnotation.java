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
import java.util.ArrayList;
import java.util.List;

import org.jboss.classfilewriter.WritableEntry;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

/**
 * A bytecode representation of a java annotation
 *
 *
 * @author Stuart Douglas
 *
 */
public class ClassAnnotation implements WritableEntry {
    private final String type;

    private final int typeIndex;

    private final List<AnnotationValue> annotationValues;

    public ClassAnnotation(ConstPool constPool, String type, List<AnnotationValue> annotationValues) {
        this.type = type;
        this.typeIndex = constPool.addUtf8Entry("L" + type.replace(".","/") + ";");
        this.annotationValues = new ArrayList<AnnotationValue>(annotationValues);
    }

    public void write(ByteArrayDataOutputStream stream) throws IOException {
        stream.writeShort(typeIndex);
        stream.writeShort(annotationValues.size());
        for (AnnotationValue value : annotationValues) {
            value.write(stream);
        }
    }

    public String getType() {
        return type;
    }

    public List<AnnotationValue> getAnnotationValues() {
        return annotationValues;
    }

}
