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

import java.io.DataOutputStream;
import java.io.IOException;

import org.jboss.classfilewriter.constpool.ConstPool;

/**
 * An enum annotation value
 * 
 * @author Stuart Douglas
 * 
 */
public class EnumAnnotationValue extends AnnotationValue {

    private final int valueIndex;

    private final int typeIndex;

    public EnumAnnotationValue(ConstPool constPool, String name, Enum<?> value) {
        super(constPool, name);
        this.valueIndex = constPool.addUtf8Entry(value.name());
        this.typeIndex = constPool.addUtf8Entry(value.getDeclaringClass().getName());
    }

    public EnumAnnotationValue(ConstPool constPool, String name, String enumType, String enumValue) {
        super(constPool, name);
        this.valueIndex = constPool.addUtf8Entry(enumValue);
        this.typeIndex = constPool.addUtf8Entry(enumType);
    }

    @Override
    public char getTag() {
        return 'e';
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        stream.writeShort(typeIndex);
        stream.writeShort(valueIndex);
    }

}
