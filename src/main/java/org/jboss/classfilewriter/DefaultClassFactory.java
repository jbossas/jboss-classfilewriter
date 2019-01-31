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

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

/**
 * Default class definition factory. This factory maintains backward compatibility
 * but it doesn't work on JDK 12 and above where ClassLoader reflection magic is forbidden.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DefaultClassFactory implements ClassFactory {

    static final ClassFactory INSTANCE = new DefaultClassFactory();

    private final java.lang.reflect.Method defineClass1, defineClass2;

    private DefaultClassFactory() {
        try {
            Method[] defineClassMethods = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
                public Method[] run() throws Exception {
                    final sun.misc.Unsafe UNSAFE;
                    final long overrideOffset;
                    // first we need to grab Unsafe
                    try {
                        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                        theUnsafe.setAccessible(true);
                        UNSAFE = (Unsafe) theUnsafe.get(null);
                        overrideOffset = UNSAFE.objectFieldOffset(AccessibleObject.class.getDeclaredField("override"));
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                    // now we gain access to CL.defineClass methods
                    Class<?> cl = ClassLoader.class;
                    Method defClass1 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class });
                    Method defClass2 = cl.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
                            int.class, ProtectionDomain.class });
                    // use Unsafe to crack open both CL.defineClass() methods (instead of using setAccessible())
                    UNSAFE.putBoolean(defClass1, overrideOffset, true);
                    UNSAFE.putBoolean(defClass2, overrideOffset, true);
                    return new Method[]{defClass1, defClass2};
                }
            });
            // set methods to volatile fields
            defineClass1 = defineClassMethods[0];
            defineClass2 = defineClassMethods[1];
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException("Cannot initialize DefaultClassFactory", pae.getException());
        }
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
            java.lang.reflect.Method method;
            Object[] args;
            if (domain == null) {
                method = defineClass1;
                args = new Object[]{name, b, 0, b.length};
            } else {
                method = defineClass2;
                args = new Object[]{name, b, 0, b.length, domain};
            }
            return (Class<?>) method.invoke(loader, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
