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
package org.jboss.classfilewriter.test.bytecode;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.DescriptorUtils;

/**
 * Utility class for testing method bytecode
 *
 * @author Stuart Douglas
 *
 */
public class MethodTester<T> {

    private static int methodNo;

    private final ClassMethod method;

    private boolean created = false;

    private Method createdMethod;

    public MethodTester(Class<T> returnType, Class<?>... params) {
        ClassFile file = new ClassFile("org.jboss.classwriter.test.GeneratedClass" + methodNo++, "java.lang.Object");
        String[] nparams = new String[params.length];
        for(int i = 0; i < params.length;++i){
            nparams[i] = DescriptorUtils.makeDescriptor(params[i]);
        }
        method = file.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.STATIC), "method", DescriptorUtils
                .makeDescriptor(returnType), nparams);
    }

    public CodeAttribute getCodeAttribute() {
        return method.getCodeAttribute();
    }

    public T invoke(Object... params) {
        if (!created) {
            create();
        }
        try {
            return (T) createdMethod.invoke(null, params);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Method getMethod() {
        if (!created) {
            create();
        }
        return createdMethod;
    }

    private void create() {
        created = true;
        Class<?> clazz = method.getClassFile().define(getClass().getClassLoader());
        for (Method i : clazz.getDeclaredMethods()) {
            if (i.getName().equals("method")) {
                createdMethod = i;
                break;
            }
        }
        if (createdMethod == null) {
            throw new RuntimeException("Created method not found on class");
        }
    }

    public void dump() {
        try {
            FileOutputStream st = new FileOutputStream("/tmp/dump.class");
            st.write(this.method.getClassFile().toBytecode());
            st.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
