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
package org.jboss.classfilewriter.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DataOutputStream sub class that allows for the lazy writing of length values.
 * <p/>
 * These length values are inserted into the bytes when then final bytes are read.
 * @author Stuart Douglas
 */
public class ByteArrayDataOutputStream extends DataOutputStream {

    private final ByteArrayOutputStream bytes;
    private final List<LazySizeImpl> sizes = new ArrayList<LazySizeImpl>();

    public ByteArrayDataOutputStream(ByteArrayOutputStream bytes) {
        super(bytes);
        this.bytes = bytes;
    }

    public ByteArrayDataOutputStream() {
        this(new ByteArrayOutputStream());
    }

    public LazySize writeSize() throws IOException {
        LazySizeImpl sv = new LazySizeImpl(this.written);
        sizes.add(sv);
        writeInt(0);
        return sv;
    }

    public byte[] getBytes() {
        byte[] data = bytes.toByteArray();
        for (final LazySizeImpl i : sizes) {
            overwriteInt(data, i.position, i.value);
        }
        return data;
    }

        /**
     * overwrites a 32 bit value in the already written bytecode data
     */
    private void overwriteInt(byte[] bytecode, int offset, int value) {
        bytecode[offset] = (byte) (value >> 24);
        bytecode[offset + 1] = (byte) (value >> 16);
        bytecode[offset + 2] = (byte) (value >> 8);
        bytecode[offset + 3] = (byte) (value);
    }

    private class LazySizeImpl implements LazySize {
        private final int position;
        private int value;

        public LazySizeImpl(int position) {
            this.position = position;
        }

        @Override
        public void markEnd() {
            value = written - position - 4;
        }
    }

}
