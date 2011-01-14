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

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.DescriptorUtils;


public class ClassMethod implements WritableEntry {

    private final String returnType;
    private final String[] parameters;
    private final String name;
    private final String descriptor;
    private final int accessFlags;

    private final ClassFile classFile;

    /**
     * The index of the name into the const pool
     */
    private final short nameIndex;
    /**
     * the index of the descriptor into the const pool
     */
    private final short descriptorIndex;

    private final List<Attribute> attributes = new ArrayList<Attribute>();

    private final CodeAttribute codeAttribute;

    private final boolean constructor;

    ClassMethod(String name, String returnType, String[] parameters, int accessFlags, ClassFile classFile) {
        ConstPool constPool = classFile.getConstPool();
        this.classFile = classFile;
        this.returnType = DescriptorUtils.validateDescriptor(returnType);
        this.parameters = parameters;
        this.name = name;
        this.descriptor = DescriptorUtils.getMethodDescriptor(parameters, returnType);
        this.accessFlags = accessFlags;
        this.nameIndex = constPool.addUtf8Entry(name);
        this.descriptorIndex = constPool.addUtf8Entry(descriptor);

        if (Modifier.isAbstract(accessFlags)) {
            codeAttribute = null;
        } else {
            codeAttribute = new CodeAttribute(this, constPool);
            attributes.add(codeAttribute);
        }
        for (String param : this.parameters) {
            DescriptorUtils.validateDescriptor(param);
        }
        this.constructor = name.equals("<init>");
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(accessFlags);
        stream.writeShort(nameIndex);
        stream.writeShort(descriptorIndex);
        stream.writeShort(attributes.size());
        for (Attribute attribute : attributes) {
            attribute.write(stream);
        }
    }

    public CodeAttribute getCodeAttribute() {
        return codeAttribute;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public String getReturnType() {
        return returnType;
    }

    public String[] getParameters() {
        return parameters;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public boolean isStatic() {
        return Modifier.isStatic(accessFlags);
    }

    public ClassFile getClassFile() {
        return classFile;
    }
}
