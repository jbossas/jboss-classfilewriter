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
package org.jboss.classfilewriter.util;

import org.jboss.classfilewriter.code.CodeAttribute;

/**
 * This class is responsible for generating bytecode fragments to box/unbox whatever happens to be on the top of the stack.
 * 
 * It is the calling codes responsibility to make sure that the correct type is on the stack
 * 
 * @author Stuart Douglas
 * 
 */
public class Boxing {

    static public void boxIfNessesary(CodeAttribute ca, String desc) {
        if (desc.length() == 1) {
            char type = desc.charAt(0);
            switch (type) {
                case 'I':
                    boxInt(ca);
                    break;
                case 'J':
                    boxLong(ca);
                    break;
                case 'S':
                    boxShort(ca);
                    break;
                case 'F':
                    boxFloat(ca);
                    break;
                case 'D':
                    boxDouble(ca);
                    break;
                case 'B':
                    boxByte(ca);
                    break;
                case 'C':
                    boxChar(ca);
                    break;
                case 'Z':
                    boxBoolean(ca);
                    break;
                default:
                    throw new RuntimeException("Cannot box unkown primitive type: " + type);
            }
        }
    }

    static public CodeAttribute unbox(CodeAttribute ca, String desc) {
        char type = desc.charAt(0);
        switch (type) {
            case 'I':
                return unboxInt(ca);
            case 'J':
                return unboxLong(ca);
            case 'S':
                return unboxShort(ca);
            case 'F':
                return unboxFloat(ca);
            case 'D':
                return unboxDouble(ca);
            case 'B':
                return unboxByte(ca);
            case 'C':
                return unboxChar(ca);
            case 'Z':
                return unboxBoolean(ca);
        }
        throw new RuntimeException("Cannot unbox unkown primitive type: " + type);
    }

    static public void boxInt(CodeAttribute bc) {
        bc.invokestatic("java.lang.Integer", "valueOf", "(I)Ljava/lang/Integer;");
    }

    static public void boxLong(CodeAttribute bc) {
        bc.invokestatic("java.lang.Long", "valueOf", "(J)Ljava/lang/Long;");
    }

    static public void boxShort(CodeAttribute bc) {
        bc.invokestatic("java.lang.Short", "valueOf", "(S)Ljava/lang/Short;");
    }

    static public void boxByte(CodeAttribute bc) {
        bc.invokestatic("java.lang.Byte", "valueOf", "(B)Ljava/lang/Byte;");
    }

    static public void boxFloat(CodeAttribute bc) {
        bc.invokestatic("java.lang.Float", "valueOf", "(F)Ljava/lang/Float;");
    }

    static public void boxDouble(CodeAttribute bc) {
        bc.invokestatic("java.lang.Double", "valueOf", "(D)Ljava/lang/Double;");
    }

    static public void boxChar(CodeAttribute bc) {
        bc.invokestatic("java.lang.Character", "valueOf", "(C)Ljava/lang/Character;");
    }

    static public void boxBoolean(CodeAttribute bc) {
        bc.invokestatic("java.lang.Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
    }

    // unboxing

    static public CodeAttribute unboxInt(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "intValue", "()I");
        return bc;
    }

    static public CodeAttribute unboxLong(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "longValue", "()J");
        return bc;
    }

    static public CodeAttribute unboxShort(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "shortValue", "()S");
        return bc;
    }

    static public CodeAttribute unboxByte(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "byteValue", "()B");
        return bc;
    }

    static public CodeAttribute unboxFloat(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "floatValue", "()F");
        return bc;
    }

    static public CodeAttribute unboxDouble(CodeAttribute bc) {
        bc.checkcast("java.lang.Number");
        bc.invokevirtual("java.lang.Number", "doubleValue", "()D");
        return bc;
    }

    static public CodeAttribute unboxChar(CodeAttribute bc) {
        bc.checkcast("java.lang.Character");
        bc.invokevirtual("java.lang.Character", "charValue", "()C");
        return bc;
    }

    static public CodeAttribute unboxBoolean(CodeAttribute bc) {
        bc.checkcast("java.lang.Boolean");
        bc.invokevirtual("java.lang.Boolean", "booleanValue", "()Z");
        return bc;
    }

}
