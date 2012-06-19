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
package org.jboss.classfilewriter.test.simple;

import java.io.IOException;
import java.lang.reflect.Method;

import junit.framework.Assert;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.DuplicateMemberException;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.junit.Test;

public class MethodTest
{

	@Test
	public void testCreatingMethod() throws SecurityException,
			NoSuchMethodException, IOException
	{

		ClassFile test = new ClassFile(getClass().getName().replace('.', '/')
				+ "GEN", "java/lang/Object");
		test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
				"method1", "Ljava/lang/Object;", "I", "J");
		CodeAttribute code = test.addMethod(AccessFlag.of(AccessFlag.PUBLIC),
				"method2", "V").getCodeAttribute();
		code.ldc(100);
		code.iconst(500);
		code.ldc(1);
		code.iconst(1);
		code.pop();
		code.pop2();
		code.returnInstruction();

		Class<?> clazz = test.define(getClass().getClassLoader());
		Assert.assertEquals(getClass().getName() + "GEN", clazz.getName());

		Method method1 = clazz.getDeclaredMethod("method1", int.class,
				long.class);
		Assert.assertEquals(Object.class, method1.getReturnType());
		Assert.assertEquals(Object.class, method1.getGenericReturnType());
		Assert.assertEquals(2, method1.getParameterTypes().length);
		Assert.assertEquals(int.class, method1.getParameterTypes()[0]);
		Assert.assertEquals(long.class, method1.getParameterTypes()[1]);
		Assert.assertEquals("method1", method1.getName());

		Method method2 = clazz.getDeclaredMethod("method2");

	}

	@Test
	public void testExceptionTypes() throws SecurityException,
			NoSuchMethodException
	{

		ClassFile test = new ClassFile(getClass().getName().replace('.', '/')
				+ "ExceptionTypes", "java/lang/Object");
		test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
				"method", "Ljava/lang/Object;", "I", "J").addCheckedExceptions(
				Exception.class);
		Class<?> clazz = test.define(getClass().getClassLoader());

		Method method = clazz
				.getDeclaredMethod("method", int.class, long.class);
		Assert.assertEquals(1, method.getExceptionTypes().length);
		Assert.assertEquals(Exception.class, method.getExceptionTypes()[0]);
	}

	@Test(expected = DuplicateMemberException.class)
	public void testDuplicateMethod()
	{
		ClassFile test = new ClassFile(getClass().getName().replace('.', '/')
				+ "DuplicateMembers", "java/lang/Object");
		test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
				"method", "Ljava/lang/Object;", "I", "J");
		test.addMethod(AccessFlag.of(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
				"method", "Ljava/lang/Object;", "I", "J");
	}

	public class AA
	{

	}

}
