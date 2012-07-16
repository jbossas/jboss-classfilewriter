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

package org.jboss.classfilewriter.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * builder class used to build a lookupswitch statement.
 *
 * @author Stuart Douglas
 */
public class LookupSwitchBuilder {

    private final CodeLocation defaultLocation;
    private final AtomicReference<BranchEnd> defaultBranchEnd;
    private final List<ValuePair> values = new ArrayList<ValuePair>();

    /**
     * Builds a lookup switch statement with no specified default location.
     *
     * When the lookup switch is added to the code attribute a {@link BranchEnd} will be returned that can be used to
     * set the location.
     */
    public LookupSwitchBuilder() {
        defaultBranchEnd = new AtomicReference<BranchEnd>();
        defaultLocation = null;
    }

    /**
     * Builds  a lookup switch statement, specifying the default location
     * @param defaultLocation The default location
     */
    public LookupSwitchBuilder(final CodeLocation defaultLocation) {
        this.defaultLocation = defaultLocation;
        defaultBranchEnd = null;
    }

    /**
     * Adds a value to the table that is at a location yet to be written.
     *
     * After this lookup switch has been written then the BranchEnd can be retrieved
     * from the returned reference.
     *
     * @param value The value to add to the lookup table
     * @return A reference to the BranchEnd that will be created.
     */
    public AtomicReference<BranchEnd> add(int value) {
        final AtomicReference<BranchEnd> end = new AtomicReference<BranchEnd>();
        ValuePair vp = new ValuePair(value, end);
        values.add(vp);
        return end;
    }
    /**
     * Adds a value to the table
     *
     *
     * @param value The value to add to the lookup table
     */
    public LookupSwitchBuilder add(int value, final CodeLocation location) {
        values.add(new ValuePair(value, location));
        return this;
    }

    public CodeLocation getDefaultLocation() {
        return defaultLocation;
    }

    public AtomicReference<BranchEnd> getDefaultBranchEnd() {
        return defaultBranchEnd;
    }

    public List<ValuePair> getValues() {
        return Collections.unmodifiableList(values);
    }

    public static class ValuePair implements Comparable<ValuePair> {
        private final int value;
        private final CodeLocation location;
        private final AtomicReference<BranchEnd> branchEnd;

        public ValuePair(final int value, final AtomicReference<BranchEnd> branchEnd) {
            this.value = value;
            this.location = null;
            this.branchEnd = branchEnd;
        }
        public ValuePair(final int value, final CodeLocation location) {
            this.value = value;
            this.location = location;
            this.branchEnd = null;
        }

        @Override
        public int compareTo(final ValuePair o) {
            return Integer.valueOf(value).compareTo(Integer.valueOf(o.value));
        }

        public int getValue() {
            return value;
        }

        public CodeLocation getLocation() {
            return location;
        }

        public AtomicReference<BranchEnd> getBranchEnd() {
            return branchEnd;
        }
    }
}
