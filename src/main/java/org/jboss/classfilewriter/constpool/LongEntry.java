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

import java.io.DataOutputStream;
import java.io.IOException;

public class LongEntry extends ConstPoolEntry {

    private final long value;

    public LongEntry(long value) {
        this.value = value;
    }

    @Override
    public ConstPoolEntryType getType() {
        return ConstPoolEntryType.LONG;
    }

    @Override
    public void writeData(DataOutputStream stream) throws IOException {
        stream.writeLong(value);
    }

}
