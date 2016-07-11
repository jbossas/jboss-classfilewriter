package org.jboss.classfilewriter.test.stackmap;

import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.test.bytecode.MethodTester;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Stuart Douglas
 */
public class StackMapTestCase {

    @Test
    public void simpleStackMergeTestCase() throws NoSuchMethodException {
        MethodTester<String> mt = new MethodTester<String>(String.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);
        BranchEnd end = ca.ifeq();
        ca.newInstruction(Foo1.class);
        ca.dup();
        ca.invokespecial(Foo1.class.getConstructor());
        BranchEnd end2 = ca.gotoInstruction();
        ca.branchEnd(end);
        ca.newInstruction(Foo2.class);
        ca.dup();
        ca.invokespecial(Foo2.class.getConstructor());
        ca.branchEnd(end2);
        ca.invokevirtual(Foo.class.getMethod("foo"));
        ca.returnInstruction();
        Assert.assertEquals("foo1", mt.invoke(10));
        Assert.assertEquals("foo2", mt.invoke(0));
    }

    @Test
    public void testInterfaceStackMerge() throws NoSuchMethodException {
        MethodTester<String> mt = new MethodTester<String>(String.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);
        BranchEnd end = ca.ifeq();
        ca.newInstruction(Bar1.class);
        ca.dup();
        ca.invokespecial(Bar1.class.getConstructor());
        BranchEnd end2 = ca.gotoInstruction();
        ca.branchEnd(end);
        ca.newInstruction(Bar2.class);
        ca.dup();
        ca.invokespecial(Bar2.class.getConstructor());
        ca.branchEnd(end2);
        ca.invokeinterface(Bar.class.getMethod("bar"));
        ca.returnInstruction();
        Assert.assertEquals("bar1", mt.invoke(10));
        Assert.assertEquals("bar2", mt.invoke(0));
    }

    @Test
    public void simpleLocalVariableMergeTestCase() throws NoSuchMethodException {
        MethodTester<String> mt = new MethodTester<String>(String.class, int.class);
        CodeAttribute ca = mt.getCodeAttribute();
        ca.iload(0);
        BranchEnd end = ca.ifeq();
        ca.newInstruction(Foo1.class);
        ca.dup();
        ca.invokespecial(Foo1.class.getConstructor());
        ca.astore(1);
        BranchEnd end2 = ca.gotoInstruction();
        ca.branchEnd(end);
        ca.newInstruction(Foo2.class);
        ca.dup();
        ca.invokespecial(Foo2.class.getConstructor());
        ca.astore(1);
        ca.branchEnd(end2);
        ca.aload(1);
        ca.invokevirtual(Foo.class.getMethod("foo"));
        ca.returnInstruction();
        Assert.assertEquals("foo1", mt.invoke(10));
        Assert.assertEquals("foo2", mt.invoke(0));
    }
}
