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

import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;
import org.jboss.classfilewriter.util.LazySize;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void writeData(ByteArrayDataOutputStream stream) throws IOException {
        LazySize sizeMarker = stream.writeSize();
        stream.writeByte(noParameters);
        for(int i = 0; i < noParameters; ++ i) {
            if(!annotations.containsKey(i)) {
                stream.writeShort(0);
            } else {
                List<ClassAnnotation> ans = annotations.get(i);
                stream.writeShort(ans.size());
                for (ClassAnnotation annotation : ans) {
                    annotation.write(stream);
                }
            }
        }
        sizeMarker.markEnd();
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
