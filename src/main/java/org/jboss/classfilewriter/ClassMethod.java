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
import org.jboss.classfilewriter.attributes.ExceptionsAttribute;
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

    private final ExceptionsAttribute exceptionsAttribute;

    private final boolean constructor;

    ClassMethod(String name, String returnType, String[] parameters, int accessFlags, ClassFile classFile) {
        ConstPool constPool = classFile.getConstPool();
        this.classFile = classFile;
        this.returnType = DescriptorUtils.validateDescriptor(returnType);
        this.parameters = parameters;
        this.name = name;
        this.descriptor = DescriptorUtils.methodDescriptor(parameters, returnType);
        this.accessFlags = accessFlags;
        this.nameIndex = constPool.addUtf8Entry(name);
        this.descriptorIndex = constPool.addUtf8Entry(descriptor);
        this.constructor = name.equals("<init>");
        this.exceptionsAttribute = new ExceptionsAttribute(constPool);
        this.attributes.add(exceptionsAttribute);

        if (Modifier.isAbstract(accessFlags)) {
            codeAttribute = null;
        } else {
            codeAttribute = new CodeAttribute(this, constPool);
            attributes.add(codeAttribute);
        }
        for (String param : this.parameters) {
            DescriptorUtils.validateDescriptor(param);
        }
    }

    public void addCheckedExceptions(Class<? extends Exception>... exceptions) {
        for (Class<? extends Exception> exception : exceptions) {
            exceptionsAttribute.addExceptionClass(exception.getName());
        }
    }

    public void addCheckedExceptions(String... exceptions) {
        for (String exception : exceptions) {
            exceptionsAttribute.addExceptionClass(exception);
        }
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + accessFlags;
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
        ClassMethod other = (ClassMethod) obj;
        if (accessFlags != other.accessFlags)
            return false;
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

}
