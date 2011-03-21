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
package org.jboss.classfilewriter.annotations;

import org.jboss.classfilewriter.WritableEntry;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        this.typeIndex = constPool.addClassEntry(type);
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
