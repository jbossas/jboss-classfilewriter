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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.classfilewriter.annotations.AnnotationBuilder;
import org.jboss.classfilewriter.annotations.AnnotationsAttribute;
import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.DescriptorUtils;

/**
 *
 * @author Stuart Douglas
 *
 */
public class ClassFile implements WritableEntry {

    private final String name;

    private final String superclass;

    private final int accessFlags;

    private final int version;

    private final ConstPool constPool = new ConstPool();

    private final List<String> interfaces = new ArrayList<String>();

    private final Set<ClassField> fields = new HashSet<ClassField>();

    private final Set<ClassMethod> methods = new HashSet<ClassMethod>();

    private byte[] bytecode;

    private final List<Attribute> attributes = new ArrayList<Attribute>();

    private final AnnotationsAttribute runtimeVisibleAnnotationsAttribute;

    public ClassFile(String name, String superclass, String... interfaces) {
        this.version = JavaVersions.JAVA_6;
        this.name = name.replace('/', '.'); // store the name in . form
        this.superclass = superclass;
        this.accessFlags = AccessFlag.of(AccessFlag.SUPER, AccessFlag.PUBLIC);
        for (String i : interfaces) {
            this.interfaces.add(i);
        }
        runtimeVisibleAnnotationsAttribute = new AnnotationsAttribute(AnnotationsAttribute.Type.RUNTIME_VISIBLE, constPool);
        this.attributes.add(runtimeVisibleAnnotationsAttribute);
    }

    public void addInterface(String iface) {
        this.interfaces.add(iface);
    }

    // fields
    /**
     * Adds a field with the given name and descriptor.
     *
     */
    public ClassField addField(int accessFlags, String name, String descriptor) {
        return addField(accessFlags, name, descriptor, null);
    }

    public ClassField addField(int accessFlags, String name, String descriptor, String signature) {
        ClassField field = new ClassField((short) accessFlags, name, descriptor, signature, this, constPool);
        if (fields.contains(field)) {
            throw new DuplicateMemberException("Field  already exists. Field: " + name + " Descriptor:" + signature);
        }
        fields.add(field);
        return field;
    }

    public ClassField addField(int accessFlags, String name, Class<?> type) {
        return addField(accessFlags, name, DescriptorUtils.makeDescriptor(type));
    }

    public ClassField addField(int accessFlags, String name, Class<?> type, Type genericType) {
        return addField(accessFlags, name, DescriptorUtils.makeDescriptor(type), null);
    }

    public ClassField addField(Field field) {
        ClassField classField = addField((short) field.getModifiers(), field.getName(), field.getType(), field.getGenericType());
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            classField.getRuntimeVisibleAnnotationsAttribute().addAnnotation(
                    AnnotationBuilder.createAnnotation(constPool, annotation));
        }
        return classField;

    }

    // methods

    public ClassMethod addMethod(int accessFlags, String name, String returnType, String... parameters) {
        ClassMethod method = new ClassMethod(name, returnType, parameters, accessFlags, this);
        if (methods.contains(method)) {
            throw new DuplicateMemberException("Method  already exists. Method: " + name + " Parameters:"
                    + Arrays.toString(parameters) + " Return Type: " + returnType);
        }
        methods.add(method);
        return method;
    }

    /**
     * Adds a method with the same signiture as the given method, including exception types
     * <p>
     * The new method will have the same modifier as the original method, except that the abstract and native flags will be
     * stripped.
     * <p>
     * TODO: annotations and signiture attribute
     */
    public ClassMethod addMethod(Method method) {
        ClassMethod classMethod = addMethod(method.getModifiers() & (~AccessFlag.ABSTRACT) & (~AccessFlag.NATIVE), method
                .getName(), DescriptorUtils.makeDescriptor(method.getReturnType()), DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
        for (Class<?> e : method.getExceptionTypes()) {
            classMethod.addCheckedExceptions((Class<? extends Exception>) e);
        }
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            classMethod.getRuntimeVisibleAnnotationsAttribute().addAnnotation(
                    AnnotationBuilder.createAnnotation(constPool, annotation));
        }
        return classMethod;
    }

    /**
     * Adds a constructor with the same signiture as the given constrcutor, including exception types
     * <p>
     * TODO: annotations and signiture attribute
     */
    public ClassMethod addConstructor(Constructor<?> method) {
        ClassMethod classMethod = addMethod(method.getModifiers(), "<init>", "V", DescriptorUtils.parameterDescriptors(method
                .getParameterTypes()));
        for (Class<?> e : method.getExceptionTypes()) {
            classMethod.addCheckedExceptions((Class<? extends Exception>) e);
        }
        for (Annotation annotation : method.getDeclaredAnnotations()) {
            classMethod.getRuntimeVisibleAnnotationsAttribute().addAnnotation(
                    AnnotationBuilder.createAnnotation(constPool, annotation));
        }
        return classMethod;
    }

    public void write(DataOutputStream stream) throws IOException {
        // first make sure everything we need is in the const pool
        short nameIndex = constPool.addClassEntry(name);
        short superClassIndex = constPool.addClassEntry(superclass);

        List<Short> interfaceIndexes = new ArrayList<Short>(interfaces.size());
        for (String i : interfaces) {
            interfaceIndexes.add(constPool.addClassEntry(i));
        }

        stream.writeInt(0xCAFEBABE);// magic
        stream.writeInt(version);
        constPool.write(stream);
        stream.writeShort(accessFlags);
        stream.writeShort(nameIndex);
        stream.writeShort(superClassIndex);
        stream.writeShort(interfaceIndexes.size()); // interface count
        for (short i : interfaceIndexes) {
            stream.writeShort(i);
        }
        stream.writeShort(fields.size()); // field count
        for (ClassField field : fields) {
            field.write(stream);
        }
        stream.writeShort(methods.size()); // method count
        for (ClassMethod method : methods) {
            method.write(stream);
        }
        stream.writeShort(attributes.size()); // attribute count
        for (Attribute attribute : attributes) {
            attribute.write(stream);
        }
    }

    private static java.lang.reflect.Method defineClass1, defineClass2;

    static {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    Class<?> cl = Class.forName("java.lang.ClassLoader", false, null);
                    defineClass1 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class });
                    defineClass1.setAccessible(true);
                    defineClass2 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class, ProtectionDomain.class });
                    defineClass2.setAccessible(true);
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

    /**
     * Definines the class using the given ClassLoader and ProtectionDomain
     */
    public Class<?> define(ClassLoader loader, ProtectionDomain domain) {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                String packageName = name.substring(0, name.lastIndexOf('.'));
                RuntimePermission permission = new RuntimePermission("defineClassInPackage." + packageName);
                sm.checkPermission(permission);
            }
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
            Class<?> clazz = (Class<?>) method.invoke(loader, args);
            return clazz;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toBytecode() {
        // TODO: throw illegal state exception if the class file is modified after writing
        if (bytecode == null) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                write(out);
                bytecode = bytes.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return bytecode;
    }

    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * returns the type descriptor for the class
     * 
     * @return
     */
    public String getDescriptor() {
        return 'L' + name + ';';
    }

    public AnnotationsAttribute getRuntimeVisibleAnnotationsAttribute() {
        return runtimeVisibleAnnotationsAttribute;
    }

}
