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

import com.zerogc.util.ByteSlice;
import com.zerogc.util.ByteUtils;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Benoit Jardin
 */

public class Comparator {
	// Dummy comparator to keep IDE happy
	public static class _KeyName_Comparator {
		public boolean less(_KeyType_ left, _KeyType_ right) { throw new NotImplementedException(); }
		public int compare(_KeyType_ left, _KeyType_ right) { throw new NotImplementedException(); }
		public boolean equals(_KeyType_ left, _KeyType_ right) { throw new NotImplementedException(); }
        public boolean equals(_KeyType_ left, byte[] rightArray, int rightOffset, int rightLength) { throw new NotImplementedException(); }
		public int hashCode(_KeyType_ value) { throw new NotImplementedException(); }
		// Special case for ByteSlice
        public int hashCode(byte[] array, int offset, int length) { throw new NotImplementedException(); }
	}
	
	public static class IntComparator {
		public boolean less(int left, int right) {
			return left < right;
		}
		public int compare(int left, int right) {
			return left < right ? -1 : (left == right ? 0 : 1);
		}
		public boolean equals(int left, int right) {
			return left == right;
		}
		public int hashCode(int value) {
			return value;
		}
	}

	public static class LongComparator {
		public boolean less(long left, long right) {
			return left < right;
		}
		public int compare(long left, long right) {
			return left < right ? -1 : (left == right ? 0 : 1);
		}
		public boolean equals(long left, long right) {
			return left == right;
		}
		public int hashCode(long value) {
			return (int)(value ^ (value >>> 32));
		}
	}
	
   public static class DoubleComparator {
		public boolean less(double left, double right) {
			return left < right;
		}
        public int compare(double left, double right) {
            return left < right ? -1 : (left == right ? 0 : 1);
        }
        public boolean equals(double left, double right) {
            return left == right;
        }
        public int hashCode(double value) {
            long bits = Double.doubleToLongBits(value);
            return (int)(bits ^ (bits >>> 32));
        }
    }

	public static class ObjectComparator {
		public boolean less(Object left, Object right) {
			return ((Comparable)left).compareTo(right) < 0;
		}
		public int compare(Object left, Object right) {
			return ((Comparable)left).compareTo(right);
		}
		public boolean equals(Object left, Object right) {
			return left.equals(right);
		}
		public int hashCode(Object value) {
			return value.hashCode();
		}
	}
	
	public static class ByteSliceComparator {
		public boolean less(byte[] leftBuffer, int leftOffset, int leftLength, byte[] rightBuffer, int rightOffset, int rightLength) {
			return ByteUtils.compareTo(leftBuffer,  leftOffset, leftLength, rightBuffer, rightOffset, rightLength) < 0;
		}
		public int compare(byte[] leftBuffer, int leftOffset, int leftLength, byte[] rightBuffer, int rightOffset, int rightLength) {
			return ByteUtils.compareTo(leftBuffer,  leftOffset, leftLength, rightBuffer, rightOffset, rightLength);
		}
		public boolean equals(byte[] leftBuffer, int leftOffset, int leftLength, byte[] rightBuffer, int rightOffset, int rightLength) {
			return ByteUtils.compareTo(leftBuffer,  leftOffset, leftLength, rightBuffer, rightOffset, rightLength) == 0;
		}
		public boolean equals(ByteSlice sliceLeft, byte[] rightBuffer, int rightOffset, int rightLength) {
			return ByteUtils.compareTo(sliceLeft.getBuffer(), sliceLeft.getOffset(), sliceLeft.getLength(), rightBuffer, rightOffset, rightLength) == 0;
		}

		public int hashCode(byte[] buffer, int offset, int length) {
			return ByteUtils.hashCode(buffer, offset, length);
		}
		public int hashCode(ByteSlice slice) {
			return ByteUtils.hashCode(slice.getBuffer(), slice.getOffset(), slice.getLength());
		}

	}
}
