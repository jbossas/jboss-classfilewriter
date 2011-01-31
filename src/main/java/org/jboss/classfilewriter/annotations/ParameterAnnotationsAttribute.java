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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * A parameter annotations attribute
 * 
 * @author Stuart Douglas
 * 
 */
public class ParameterAnnotationsAttribute extends Attribute {

    public static enum Type {
        RUNTIME_VISIBLE("RuntimeVisibleParameterAnnotations"), RUNTIME_INVISIBLE("RuntimeInvisibleParameterAnnotations");

        private Type(String tag) {
            this.tag = tag;
        }

        private final String tag;

        public String getTag() {
            return tag;
        }
    }

    private final Map<Integer, List<ClassAnnotation>> annotations;
    private final int noParameters;

    public ParameterAnnotationsAttribute(Type type, ConstPool constPool, int noParameters) {
        super(type.getTag(), constPool);
        this.annotations = new HashMap<Integer, List<ClassAnnotation>>();
        this.noParameters = noParameters;
    }


    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        for(int i = 0; i < noParameters; ++ i) {
            if(!annotations.containsKey(i)) {
                dos.writeShort(0);
            } else {
                List<ClassAnnotation> ans = annotations.get(i);
                dos.writeShort(ans.size());
                for (ClassAnnotation annotation : ans) {
                    annotation.write(dos);
                }
            }
        }
        byte[] data = bos.toByteArray();
        stream.writeInt(data.length + 1);
        stream.writeByte(noParameters);
        stream.write(data);
    }

    public void addAnnotation(int parameter, Annotation annotation) {
        if (!annotations.containsKey(parameter)) {
            annotations.put(parameter, new ArrayList<ClassAnnotation>());
        }
        annotations.get(parameter).add(AnnotationBuilder.createAnnotation(constPool, annotation));
    }

    public void addAnnotation(int parameter, ClassAnnotation annotation) {
        if (!annotations.containsKey(parameter)) {
            annotations.put(parameter, new ArrayList<ClassAnnotation>());
        }
        annotations.get(parameter).add(annotation);
    }

}
