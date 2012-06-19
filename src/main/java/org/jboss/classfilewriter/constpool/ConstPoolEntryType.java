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
