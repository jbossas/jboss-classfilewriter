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
package org.jboss.classfilewriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.classfilewriter.annotations.AnnotationBuilder;
import org.jboss.classfilewriter.annotations.AnnotationsAttribute;
import org.jboss.classfilewriter.attributes.Attribute;
import org.jboss.classfilewriter.constpool.ConstPool;
import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;
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

    private final ClassLoader classLoader;

    public ClassFile(String name, String superclass, String... interfaces) {
        this(name, AccessFlag.of(AccessFlag.SUPER, AccessFlag.PUBLIC), superclass, null, interfaces);
    }

    public ClassFile(String name, int accessFlags, String superclass, String... interfaces) {
        this(name, accessFlags, superclass, null, interfaces);
    }

    public ClassFile(String name, String superclass, ClassLoader classLoader, String... interfaces) {
        this(name, AccessFlag.of(AccessFlag.SUPER, AccessFlag.PUBLIC), superclass, classLoader, interfaces);
    }

    public ClassFile(String name, int accessFlags, String superclass, ClassLoader classLoader, String... interfaces) {
        this(name, accessFlags, superclass, JavaVersions.JAVA_6, classLoader, interfaces);
    }

    public ClassFile(String name, int accessFlags, String superclass, int version, ClassLoader classLoader, String... interfaces) {
        if(version > JavaVersions.JAVA_6 && classLoader == null) {
            throw new IllegalArgumentException("ClassLoader must be specified if version is greater than Java 6");
        }
        this.version = version;
        this.classLoader = classLoader;
        this.name = name.replace('/', '.'); // store the name in . form
        this.superclass = superclass;
        this.accessFlags = accessFlags;
        this.interfaces.addAll(Arrays.asList(interfaces));
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
        ClassField field = new ClassField((short) accessFlags, name, descriptor, this, constPool);
        if (fields.contains(field)) {
            throw new DuplicateMemberException("Field  already exists. Field: " + name + " Descriptor:" + signature);
        }
        fields.add(field);
        field.setSignature(signature);
        return field;
    }

    public ClassField addField(int accessFlags, String name, Class<?> type) {
        return addField(accessFlags, name, DescriptorUtils.makeDescriptor(type));
    }

    public ClassField addField(int accessFlags, String name, Class<?> type, String genericSignature) {
        return addField(accessFlags, name, DescriptorUtils.makeDescriptor(type), genericSignature);
    }

    public ClassField addField(Field field) {
        ClassField classField = addField((short) field.getModifiers(), field.getName(), field.getType(), null);
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
        int count = 0;
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation annotation : parameterAnnotations) {
                classMethod.getRuntimeVisibleParameterAnnotationsAttribute().addAnnotation(count,
                        AnnotationBuilder.createAnnotation(constPool, annotation));
            }
            count++;
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
        int count = 0;
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation annotation : parameterAnnotations) {
                classMethod.getRuntimeVisibleParameterAnnotationsAttribute().addAnnotation(count,
                        AnnotationBuilder.createAnnotation(constPool, annotation));
            }
            count++;
        }
        return classMethod;
    }

    public void write(ByteArrayDataOutputStream stream) throws IOException {
        // first make sure everything we need is in the const pool
        int nameIndex = constPool.addClassEntry(name);
        int superClassIndex = constPool.addClassEntry(superclass);

        List<Integer> interfaceIndexes = new ArrayList<Integer>(interfaces.size());
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
        for (int i : interfaceIndexes) {
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

    public Class<?> define() {
        return defineInternal(classLoader, null);
    }

    @Deprecated
    public Class<?> define(ClassLoader loader) {
        return defineInternal(loader, null);
    }

    public Class<?> define(ProtectionDomain domain) {
        return defineInternal(classLoader, domain);
    }
    /**
     * Definines the class using the given ClassLoader and ProtectionDomain
     */
    @Deprecated
    public Class<?> define(ClassLoader loader, ProtectionDomain domain) {
        return defineInternal(loader, domain);
    }

    private Class<?> defineInternal(ClassLoader loader, ProtectionDomain domain) {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final int index = name.lastIndexOf('.');
                final String packageName;
                if(index == -1 ) {
                    packageName = "";
                } else {
                    packageName = name.substring(0, index);
                }
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
                ByteArrayDataOutputStream out = new ByteArrayDataOutputStream();
                write(out);
                bytecode = out.getBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return bytecode;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
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
        return DescriptorUtils.makeDescriptor(name);
    }

    public AnnotationsAttribute getRuntimeVisibleAnnotationsAttribute() {
        return runtimeVisibleAnnotationsAttribute;
    }

    /**
     * Returns the generated class name
     *
     * @return The generated class name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return The generated superclass name
     */
    public String getSuperclass() {
        return superclass;
    }

    /**
     *
     * @return The interfaces implemented by this class
     */
    public List<String> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     *
     * @return This class's fields
     */
    public Set<ClassField> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    /**
     *
     * @return This classes methods
     */
    public Set<ClassMethod> getMethods() {
        return Collections.unmodifiableSet(methods);
    }

}
