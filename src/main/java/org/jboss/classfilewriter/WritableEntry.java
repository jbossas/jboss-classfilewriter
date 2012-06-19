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
package org.jboss.classfilewriter;

import org.jboss.classfilewriter.util.ByteArrayDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * a part of the class file that knows hows to write itself to a {@link ByteArrayOutputStream}
 *
 * @author Stuart Douglas
 *
 */
public interface WritableEntry {
    void write(ByteArrayDataOutputStream stream) throws IOException;
}
