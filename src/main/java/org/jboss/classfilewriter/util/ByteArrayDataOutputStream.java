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
