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
package com.stuartdouglas.classfilewriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import com.stuartdouglas.classfilewriter.constpool.ConstPool;
import com.stuartdouglas.classfilewriter.util.DescriptorUtils;
import com.stuartdouglas.classfilewriter.util.SignatureBuilder;

/**
 *
 * @author Stuart Douglas
 *
 */
public class ClassFile implements WritableEntry {

    private final String name;

    private final String superclass;

    private int modifiers;

    private int version = JavaVersions.JAVA_5;

    private final ConstPool constPool = new ConstPool();

    private final List<ClassField> fields = new ArrayList<ClassField>();

    public ClassFile(String name, String superclass) {
        this.name = name;
        this.superclass = superclass;
        this.modifiers = AccessFlag.of(AccessFlag.SUPER, AccessFlag.PUBLIC);
    }

    // fields
    /**
     * Adds a field with the given name and descriptor.
     *
     */
    public ClassField addField(String name, String descriptor, int accessFlags) {
        return addField(name, descriptor, accessFlags, null);
    }

    // TODO: signature attribute
    public ClassField addField(String name, String descriptor, int accessFlags, String signature) {
        ClassField field = new ClassField((short) accessFlags, name, descriptor, signature, this, constPool);
        fields.add(field);
        return field;
    }

    public ClassField addField(String name, Class<?> type, int accessFlags) {
        return addField(name, DescriptorUtils.classToStringRepresentation(type), accessFlags);
    }

    public ClassField addField(String name, Class<?> type, Type genericType, int accessFlags) {
        return addField(name, DescriptorUtils.classToStringRepresentation(type), accessFlags, SignatureBuilder
                .fieldAttribute(genericType));
    }

    public ClassField addField(Field field) {
        // TODO: signiture
        return addField(field.getName(), DescriptorUtils.classToStringRepresentation(field.getType()), (short) field
                .getModifiers());
    }

    public void write(DataOutputStream stream) throws IOException {
        // first make sure everything we need is in the const pool
        short nameIndex = constPool.addClassEntry(name);
        short superClassIndex = constPool.addClassEntry(superclass);

        stream.writeInt(0xCAFEBABE);// magic
        stream.writeInt(version);
        constPool.write(stream);
        stream.writeShort(modifiers);
        stream.writeShort(nameIndex);
        stream.writeShort(superClassIndex);
        stream.writeShort(0); // interface count
        stream.writeShort(fields.size()); // field count
        for (ClassField field : fields) {
            field.write(stream);
        }
        stream.writeShort(0); // method count
        stream.writeShort(0); // attribute count
    }

    private static java.lang.reflect.Method defineClass1, defineClass2;

    static {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    Class<?> cl = Class.forName("java.lang.ClassLoader");
                    defineClass1 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class });

                    defineClass2 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class, ProtectionDomain.class });
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("cannot initialize ClassFile", pae.getException());
        }
    }

    public Class<?> define(ClassLoader loader) {
        return define(loader, null);
    }

    public Class<?> define(ClassLoader loader, ProtectionDomain domain) {
        try {
            byte[] b = toBytecode();
            java.lang.reflect.Method method;
            Object[] args;
            if (domain == null) {
                method = defineClass1;
                args = new Object[] { name.replace('/', '.'), b, new Integer(0), new Integer(b.length) };
            } else {
                method = defineClass2;
                args = new Object[] { name.replace('/', '.'), b, new Integer(0), new Integer(b.length), domain };
            }
            method.setAccessible(true);
            Class<?> clazz = Class.class.cast(method.invoke(loader, args));
            method.setAccessible(false);
            return clazz;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toBytecode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            write(out);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
