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
package org.jboss.classfilewriter.constpool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.classfilewriter.WritableEntry;


public class ConstPool implements WritableEntry {

    private final LinkedHashMap<Short, ConstPoolEntry> entries = new LinkedHashMap<Short, ConstPoolEntry>();

    private final Map<String, Short> utf8Locations = new HashMap<String, Short>();
    private final Map<String, Short> classLocations = new HashMap<String, Short>();
    private final Map<String, Short> stringLocations = new HashMap<String, Short>();
    private final Map<NameAndType, Short> nameAndTypeLocations = new HashMap<NameAndType, Short>();
    private final Map<MemberInfo, Short> fieldLocations = new HashMap<MemberInfo, Short>();
    private final Map<MemberInfo, Short> methodLocations = new HashMap<MemberInfo, Short>();
    private final Map<MemberInfo, Short> interfaceMethodLocations = new HashMap<MemberInfo, Short>();
    private final Map<Integer, Short> integerLocations = new HashMap<Integer, Short>();
    private final Map<Float, Short> floatLocations = new HashMap<Float, Short>();
    private final Map<Long, Short> longLocations = new HashMap<Long, Short>();
    private final Map<Double, Short> doubleLocations = new HashMap<Double, Short>();

    private short count = 1;

    /**
     * The constant_pool_count field of the class file format
     */
    private short constPoolSize = 1;

    public short addUtf8Entry(String entry) {
        if (utf8Locations.containsKey(entry)) {
            return utf8Locations.get(entry);
        }
        final short index = count++;
        constPoolSize++;
        entries.put(index, new Utf8Entry(entry));
        utf8Locations.put(entry, index);
        return index;
    }

    /**
     * Adds a CONSTANT_Class_info to the const pool. This must be in internal form
     */
    public short addClassEntry(String className) {
        className = className.replace('.', '/');
        if (classLocations.containsKey(className)) {
            return classLocations.get(className);
        }
        final short utf8Location = addUtf8Entry(className);
        final short index = count++;
        constPoolSize++;
        entries.put(index, new ClassEntry(utf8Location));
        classLocations.put(className, index);
        return index;
    }

    /**
     * Adds a CONSTANT_String_info to the const pool.
     */
    public short addStringEntry(String string) {
        if (stringLocations.containsKey(string)) {
            return stringLocations.get(string);
        }
        final short utf8Location = addUtf8Entry(string);
        final short index = count++;
        constPoolSize++;
        entries.put(index, new StringEntry(utf8Location));
        stringLocations.put(string, index);
        return index;
    }

    public short addIntegerEntry(int entry) {
        if (integerLocations.containsKey(entry)) {
            return integerLocations.get(entry);
        }
        final short index = count++;
        constPoolSize++;
        entries.put(index, new IntegerEntry(entry));
        integerLocations.put(entry, index);
        return index;
    }

    public short addFloatEntry(float entry) {
        if (floatLocations.containsKey(entry)) {
            return floatLocations.get(entry);
        }
        final short index = count++;
        constPoolSize++;
        entries.put(index, new FloatEntry(entry));
        floatLocations.put(entry, index);
        return index;
    }

    public short addLongEntry(long entry) {
        if (longLocations.containsKey(entry)) {
            return longLocations.get(entry);
        }
        final short index = count++;
        count++;
        constPoolSize += 2;
        entries.put(index, new LongEntry(entry));
        longLocations.put(entry, index);
        return index;
    }

    public short addDoubleEntry(double entry) {
        if (doubleLocations.containsKey(entry)) {
            return doubleLocations.get(entry);
        }
        final short index = count++;
        count++;
        constPoolSize += 2;
        entries.put(index, new DoubleEntry(entry));
        doubleLocations.put(entry, index);
        return index;
    }

    public short addNameAndTypeEntry(String name, String type) {
        final NameAndType typeInfo = new NameAndType(name, type);
        if(nameAndTypeLocations.containsKey(typeInfo)) {
            return nameAndTypeLocations.get(typeInfo);
        }
        final short nameIndex = addUtf8Entry(name);
        final short typeIndex = addUtf8Entry(type);
        final short index = count++;
        constPoolSize++;
        entries.put(index,new NameAndTypeEntry(nameIndex, typeIndex));
        nameAndTypeLocations.put(typeInfo, index);
        return index;
    }

    public short addFieldEntry(String className, String fieldName, String fieldType) {
        final NameAndType nameAndType = new NameAndType(fieldName, fieldType);
        final MemberInfo field = new MemberInfo(className, nameAndType);
        if (fieldLocations.containsKey(field)) {
            return fieldLocations.get(field);
        }
        final short nameAndTypeIndex = addNameAndTypeEntry(fieldName, fieldType);
        final short classIndex = addClassEntry(className);
        final short index = count++;
        constPoolSize++;
        entries.put(index, new FieldRefEntry(classIndex, nameAndTypeIndex));
        fieldLocations.put(field, index);
        return index;
    }

    public short addMethodEntry(String className, String methodName, String descriptor) {
        final NameAndType nameAndType = new NameAndType(methodName, descriptor);
        final MemberInfo method = new MemberInfo(className, nameAndType);
        if (methodLocations.containsKey(method)) {
            return methodLocations.get(method);
        }
        final short nameAndTypeIndex = addNameAndTypeEntry(methodName, descriptor);
        final short classIndex = addClassEntry(className);
        final short index = count++;
        constPoolSize++;
        entries.put(index, new MethodRefEntry(classIndex, nameAndTypeIndex));
        methodLocations.put(method, index);
        return index;
    }

    public short addInterfaceMethodEntry(String className, String methodName, String descriptor) {
        final NameAndType nameAndType = new NameAndType(methodName, descriptor);
        final MemberInfo method = new MemberInfo(className, nameAndType);
        if (interfaceMethodLocations.containsKey(method)) {
            return interfaceMethodLocations.get(method);
        }
        final short nameAndTypeIndex = addNameAndTypeEntry(methodName, descriptor);
        final short classIndex = addClassEntry(className);
        final short index = count++;
        constPoolSize++;
        entries.put(index, new InterfaceMethodRefEntry(classIndex, nameAndTypeIndex));
        interfaceMethodLocations.put(method, index);
        return index;
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(constPoolSize);
        for (Entry<Short, ConstPoolEntry> entry : entries.entrySet()) {
            entry.getValue().write(stream);
        }
    }

    private static final class NameAndType {
        private final String name, type;

        public NameAndType(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NameAndType other = (NameAndType) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }

    private static class MemberInfo {
        private final String className;
        private final NameAndType nameAndType;

        public MemberInfo(String className,NameAndType nameAndType) {
            this.className = className;
            this.nameAndType = nameAndType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((nameAndType == null) ? 0 : nameAndType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MemberInfo other = (MemberInfo) obj;
            if (className == null) {
                if (other.className != null)
                    return false;
            } else if (!className.equals(other.className))
                return false;
            if (nameAndType == null) {
                if (other.nameAndType != null)
                    return false;
            } else if (!nameAndType.equals(other.nameAndType))
                return false;
            return true;
        }

    }
}
