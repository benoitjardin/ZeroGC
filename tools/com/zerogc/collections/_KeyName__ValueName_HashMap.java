/*
 * Copyright 2016 Benoit Jardin.
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
package com.zerogc.collections;
import com.zerogc.core.ByteSlice;

public class _KeyName__ValueName_HashMap extends _KeyName_HashSet {
    private _ValueType_[] value;

    @Override
    protected void grow(int capacity, int newCapacity) {
        super.grow(capacity, newCapacity);

        _ValueType_[] newValue = new _ValueType_[newCapacity];
        if (capacity > 0) {
            System.arraycopy(this.value, 0, newValue, 0, capacity);
        }
        this.value = newValue;
    }

    public _KeyName__ValueName_HashMap() {
        this(_KeyName__ValueName_HashMap.class.getSimpleName(), INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public _KeyName__ValueName_HashMap(String name) {
        this(name, INITIAL_CAPACITY, GROWTH_FACTOR);
    }

    public _KeyName__ValueName_HashMap(String name, int initialCapacity) {
        this(name, initialCapacity, GROWTH_FACTOR);
    }

    public _KeyName__ValueName_HashMap(String name, int initialCapacity, float growthFactor) {
        super(name, initialCapacity, growthFactor);
    }

    public final _ValueType_ getValue(int entry) {
        return this.value[entry];
    }

    public final _ValueType_ get(_KeyType_ key) {
        return this.value[find(key)];
    }

    public final int insert(_KeyType_ key, _ValueType_ value) {
        int entry = super.insert(key);
        this.value[entry] = value;
        return entry;
    }
}
