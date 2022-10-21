/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2019 Red Hat, Inc.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

/**
 * Default class definition factory.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DefaultClassFactory implements ClassFactory {

    private static final String DEFINE_CLASS_METHOD_NAME = "defineClass";
    private static final MethodHandle defineClassWithoutDomainParam, defineClassWithDomainParam;

    static {
        MethodHandle[] defineClassMethods;
        try {
            MethodHandles.Lookup LOOKUP = MethodHandles.privateLookupIn(ClassLoader.class, MethodHandles.lookup());
            defineClassMethods = AccessController.doPrivileged(new PrivilegedExceptionAction<>() {
                public MethodHandle[] run() throws Exception {
                    MethodHandle defineClass1 = LOOKUP.findVirtual(ClassLoader.class, DEFINE_CLASS_METHOD_NAME,
                            MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class));
                    MethodHandle defineClass2 = LOOKUP.findVirtual(ClassLoader.class, DEFINE_CLASS_METHOD_NAME,
                            MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class));
                    return new MethodHandle[]{defineClass1, defineClass2};
                }
            });
        } catch (Throwable t) {
            throw new RuntimeException("Cannot initialize " + DefaultClassFactory.class.getName(), t);
        }
        defineClassWithoutDomainParam = defineClassMethods[0];
        defineClassWithDomainParam = defineClassMethods[1];
    }

    static final ClassFactory INSTANCE = new DefaultClassFactory();

    private DefaultClassFactory() {
        // forbidden instantiation
    }

    @Override
    public Class<?> defineClass(final ClassLoader loader, final String name,
                                final byte[] b, final int off, final int len,
                                final ProtectionDomain domain) throws ClassFormatError {
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
            if (domain == null) {
                return (Class<?>) defineClassWithoutDomainParam.invokeExact(loader, name, b, 0, b.length);
            } else {
                return (Class<?>) defineClassWithDomainParam.invokeExact(loader, name, b, 0, b.length, domain);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
