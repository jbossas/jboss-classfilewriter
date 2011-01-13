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
package com.stuartdouglas.classfilewriter.code;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;

import com.stuartdouglas.classfilewriter.ClassMethod;
import com.stuartdouglas.classfilewriter.attributes.Attribute;
import com.stuartdouglas.classfilewriter.constpool.ConstPool;
import com.stuartdouglas.classfilewriter.util.DescriptorUtils;

public class CodeAttribute extends Attribute {

    public static final String NAME = "Code";

    private final ClassMethod method;

    private final ByteArrayOutputStream finalDataBytes;

    private final DataOutputStream data;

    private int maxLocals = 0;

    private int maxStackDepth = 0;

    public CodeAttribute(ClassMethod method, ConstPool constPool) {
        super(NAME, constPool);
        this.method = method;
        this.finalDataBytes = new ByteArrayOutputStream();
        this.data = new DataOutputStream(finalDataBytes);

        if (!Modifier.isStatic(method.getAccessFlags())) {
            maxLocals++;
        }
        for (String param : method.getParameters()) {
            if (DescriptorUtils.isWide(param)) {
                maxLocals += 2;
            } else {
                maxLocals++;
            }
        }
    }

    /**
     * Adds the appropriate return instruction for the methods return type.
     */
    public void addReturnInstruction() {
        if (method.getReturnType().length() > 1) {
            writeByte(Opcode.ARETURN);
        } else {
            char ret = method.getReturnType().charAt(0);
            switch (ret) {
                case 'V':
                    writeByte(Opcode.RETURN);
                    break;
                case 'I':
                case 'Z':
                case 'S':
                case 'B':
                case 'C':
                    writeByte(Opcode.IRETURN);
                    break;
                case 'F':
                    writeByte(Opcode.FRETURN);
                    break;
                case 'D':
                    writeByte(Opcode.DRETURN);
                    break;
                case 'J':
                    writeByte(Opcode.LRETURN);
            }
        }
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        stream.writeInt(finalDataBytes.size() + 12); // attribute length
        stream.writeShort(maxStackDepth);
        stream.writeShort(maxLocals);
        stream.writeInt(finalDataBytes.size());
        stream.write(finalDataBytes.toByteArray());
        stream.writeShort(0); // exception table length
        stream.writeShort(0); // attributes count
    }

    private void writeByte(int n) {
        try {
            data.writeByte(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
