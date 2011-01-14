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

public enum ConstPoolEntryType {
    CLASS(7,1),
    FIELDREF(9,1),
    METHODREF(10,1),
    INTERFACE_METHODREF(11,1),
    STRING(8,1),
    INTEGER(3,1),
    FLOAT(4,1),
    LONG(5,2),
    DOUBLE(6,2),
    NAME_AND_TYPE(12,1),
    UTF8(1,1);

    private final int tag;
    private final int slots;

    private ConstPoolEntryType(int tag, int slots) {
        this.tag = tag;
        this.slots = slots;
    }

    public int getTag() {
        return tag;
    }

    /**
     * The number of spaces this takes up in the const pool
     */
    public int getSlots() {
        return slots;
    }
}
