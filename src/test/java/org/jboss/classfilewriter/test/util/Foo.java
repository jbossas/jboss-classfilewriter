/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.classfilewriter.test.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin Kouba
 */
public class Foo {

    class Inner<T> {
    }

    static class StaticNested {

        class Inner {
        }

    }

    @ExpectedSignature("<T:Ljava/lang/Object;>(IDJSFZBC)V")
    public <T> void multipleParamsPrimitives(int i, double d, long l, short s, float f, boolean b, byte by, char c) {
    }

    @ExpectedSignature("(Lorg/jboss/classfilewriter/test/util/Foo$Inner<Ljava/lang/String;>;)V")
    public void singleParamInnerGeneric(Inner<String> inner) {
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>(Lorg/jboss/classfilewriter/test/util/Foo$StaticNested;TT;)V")
    public <T> void singleParamStaticNested(StaticNested staticNested, T object) {
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>(Lorg/jboss/classfilewriter/test/util/Foo$StaticNested$Inner;)V")
    public <T> void singleParamStaticNestedInner(StaticNested.Inner inner) {
    }

    @ExpectedSignature("(Ljava/util/List<Ljava/lang/String;>;Ljava/lang/Integer;)V")
    public void multipleParamsSimpleGenericType(List<String> list, Integer number) {
    }

    @ExpectedSignature("([Ljava/lang/String;)V")
    public void singleParamSimpleArray(String[] array) {
    }

    @ExpectedSignature("([Ljava/util/List<Ljava/lang/Double;>;)V")
    public void singleParamGenericArray(List<Double>[] array) {
    }

    @ExpectedSignature("([[Ljava/util/List<Ljava/lang/Long;>;)V")
    public void singleParamMultidimensionalGenericArray(List<Long>[][] array) {
    }

    @ExpectedSignature("(Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Ljava/lang/Integer;>;>;>;)V")
    public void singleParamComplicatedGenericType(List<Map<String, List<Integer>>> list) {
    }

    @ExpectedSignature("(Ljava/util/List<*>;)V")
    public void singleParamSimpleWildcard(List<?> list) {
    }

    @ExpectedSignature("(Ljava/util/List<+Ljava/io/Serializable;>;)V")
    public void singleParamSimpleWildcardUpperBound(List<? extends Serializable> list) {
    }

    @ExpectedSignature("(Ljava/util/List<+Ljava/util/Map<Ljava/lang/String;*>;>;)V")
    public void singleParamSimpleWildcardUpperBoundGeneric(List<? extends Map<String, ?>> list) {
    }

    @ExpectedSignature("(Ljava/util/List<-Ljava/io/Serializable;>;)V")
    public void singleParamSimpleWildcardLoweBound(List<? super Serializable> list) {
    }

    @ExpectedSignature("<T:Ljava/lang/Object;X:Ljava/lang/Object;>(Ljava/util/List<TT;>;)V")
    public <T, X> void singleParamGenericTypeWithTypeVariable(List<T> list) {
    }

    @ExpectedSignature("<T:Ljava/lang/Throwable;>()V^Ljava/lang/RuntimeException;^Ljava/lang/Throwable;^TT;")
    public <T extends Throwable> void throwsWithTypeVariable() throws RuntimeException, Throwable, T {
    }

    @ExpectedSignature("<X:Ljava/lang/Object;T:Ljava/lang/Throwable;U::Ljava/io/Serializable;:Ljava/lang/Comparable<TT;>;>(Ljava/lang/Comparable<-TX;>;Ljava/util/List<Ljava/util/List<Ljava/util/List<TU;>;>;>;Ljava/util/Map<TT;Ljava/util/List<Lorg/jboss/classfilewriter/test/util/Foo$Inner<Ljava/lang/Comparable<Ljava/io/Serializable;>;>;>;>;TT;)Ljava/util/List<+Ljava/util/Map<*Ljava/lang/String;>;>;^Ljava/lang/Throwable;^TT;")
    public <X, T extends Throwable, U extends Serializable & Comparable<T>> List<? extends Map<?, String>> superComplicated(Comparable<? super X> comparable,
            List<List<List<U>>> list, Map<T, List<Inner<Comparable<Serializable>>>> map, T type) throws Throwable, T {
        return null;
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>()I")
    public <T> int returnTypeInt() {
        return 1;
    }

    @ExpectedSignature("()Ljava/util/List<*>;")
    public List<?> returnTypeSimpleWildcard() {
        return null;
    }

    @ExpectedSignature("()Ljava/util/List<+Ljava/io/Serializable;>;")
    public List<? extends Serializable> returnTypeSimpleWildcardUpperBound() {
        return null;
    }

    @ExpectedSignature("()Ljava/util/List<-Ljava/io/Serializable;>;")
    public List<? super Serializable> returnTypeSimpleWildcardLowerBound() {
        return null;
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>()[Ljava/lang/String;")
    public <T> String[] returnTypeSimpleArray() {
        return null;
    }

    @ExpectedSignature("()[Ljava/util/List<Ljava/lang/Double;>;")
    public List<Double>[] returnTypeSimpleGenericArray() {
        return null;
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>()[Ljava/util/List<Ljava/util/Map<TT;Ljava/lang/String;>;>;")
    public <T> List<Map<T, String>>[] returnTypeGenericArray() {
        return null;
    }

    @ExpectedSignature("()[[[Ljava/util/List<Ljava/lang/Long;>;")
    public List<Long>[][][] returnTypeMultidimensionalGenericArray() {
        return null;
    }

    @ExpectedSignature("()Lorg/jboss/classfilewriter/test/util/Foo$Inner<Ljava/lang/String;>;")
    public Inner<String> returnTypeInnerGeneric() {
        return null;
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>()Lorg/jboss/classfilewriter/test/util/Foo$StaticNested$Inner;")
    public <T> StaticNested.Inner returnTypeStaticNestedInner() {
        return null;
    }

    @ExpectedSignature("(Ljava/util/List<Ljava/lang/String;>;Ljava/lang/Integer;)V")
    public static void staticMultipleParamsSimpleGenericType(List<String> list, Integer number) {
    }

    @ExpectedSignature("<T:Ljava/lang/Object;>()Lorg/jboss/classfilewriter/test/util/Foo$StaticNested$Inner;")
    public static <T> StaticNested.Inner staticReturnTypeStaticNestedInner() {
        return null;
    }

}