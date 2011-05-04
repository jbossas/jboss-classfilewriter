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
package org.jboss.classfilewriter;

import org.jboss.classfilewriter.annotations.AnnotationsAttribute;
import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.attributes.SignatureAttribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A field in a class
 *
 * @author Stuart Douglas
 *
 */
public class ClassField implements WritableEntry {

    private final short accessFlags;
    private final String name;
    private final short nameIndex;
    private final String descriptor;
    private final short descriptorIndex;
    private final List<Attribute> attributes = new ArrayList<Attribute>();

    private final ClassFile classFile;

    private final AnnotationsAttribute runtimeVisibleAnnotationsAttribute;

    private SignatureAttribute signatureAttribute;

    private String signature;

    ClassField(short accessFlags, String name, String descriptor, ClassFile classFile,
            ConstPool constPool) {
        this.accessFlags = accessFlags;
        this.name = name;
        this.descriptor = descriptor;
        this.classFile = classFile;
        this.nameIndex = constPool.addUtf8Entry(name);
        this.descriptorIndex = constPool.addUtf8Entry(descriptor);
        runtimeVisibleAnnotationsAttribute = new AnnotationsAttribute(AnnotationsAttribute.Type.RUNTIME_VISIBLE, constPool);
        this.attributes.add(runtimeVisibleAnnotationsAttribute);
    }

    public void write(ByteArrayDataOutputStream stream) throws IOException {
        if(signatureAttribute != null) {
            attributes.add(signatureAttribute);
        }
        stream.writeShort(accessFlags);
        stream.writeShort(nameIndex);
        stream.writeShort(descriptorIndex);
        stream.writeShort(attributes.size());
        for (Attribute attribute : attributes) {
            attribute.write(stream);
        }
    }

    public short getAccessFlags() {
        return accessFlags;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        if(signature == null) {
            signatureAttribute = null;
        } else {
            signatureAttribute = new SignatureAttribute(classFile.getConstPool(), signature);
        }
        this.signature = signature;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassField other = (ClassField) obj;
        if (descriptor == null) {
            if (other.descriptor != null)
                return false;
        } else if (!descriptor.equals(other.descriptor))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public AnnotationsAttribute getRuntimeVisibleAnnotationsAttribute() {
        return runtimeVisibleAnnotationsAttribute;
    }
}
