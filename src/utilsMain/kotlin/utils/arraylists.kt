package utils

interface PrimitiveArrayList<T> : List<T> {
    fun add(value: T)
    override fun contains(element: T): Boolean {
        return indexOf(element) != -1
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (e in elements) {
            if (!contains(e)) return false
        }
        return true
    }

    override fun isEmpty() = size == 0
}

class IntArrayList(
    initialCapacity: Int = 10,
    private var capacity: Int = initialCapacity,
    private var elements: Int = 0,
    private var storage: IntArray = IntArray(initialCapacity)
) : PrimitiveArrayList<Int> {

    override val size: Int
        get() = elements

    override fun add(value: Int) {
        ensureCapacity(elements + 1)
        storage[elements++] = value
    }

    fun addAll(values: IntArrayList) {
        ensureCapacity(elements + values.size) // ensure once for all elements
        for (value in values) add(value)
    }

    fun clear() {
        elements = 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is IntArrayList) {
            if (other.elements != elements) return false
            return storage.contentEquals(other.storage)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = elements.hashCode()
        result = 31 * result + storage.contentHashCode()
        return result
    }

    override operator fun get(index: Int): Int {
        checkBounds(index)
        return storage[index]
    }

    override fun indexOf(element: Int): Int {
        for (i in 0 until elements) {
            if (storage[i] == element) {
                return i
            }
        }
        return -1
    }

    override fun lastIndexOf(element: Int): Int {
        for (i in elements - 1 downTo 0) {
            if (storage[i] == element) {
                return i
            }
        }
        return -1
    }

    override fun iterator() = Iterator()

    override fun listIterator(): ListIterator<Int> {
        throw UnsupportedOperationException("Not implemented!")
    }

    override fun listIterator(index: Int): ListIterator<Int> {
        throw UnsupportedOperationException("Not implemented!")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        val subStorage = IntArray(toIndex - fromIndex + 1)
        for (i in fromIndex..toIndex) {
            subStorage[i - fromIndex] = storage[i]
        }
        return wrap(subStorage)
    }

    private inline fun checkBounds(index: Int) {
        if (index < 0 || index >= elements) throw IllegalArgumentException("Index $index out of bounds <0; $elements)")
    }

    private inline fun ensureCapacity(requiredCapacity: Int) {
        if (capacity < requiredCapacity) {
            resizeTo(capacity * 2)
        }
    }

    private fun resizeTo(newCapacity: Int) {
        storage = storage.copyOf(newCapacity)
    }

    inner class Iterator : kotlin.collections.Iterator<Int> {
        private var idx = 0

        override fun hasNext(): Boolean {
            return idx < elements
        }

        override fun next(): Int {
            return storage[idx++]
        }
    }

    companion object {
        fun wrap(array: IntArray, initialCapacity: Int = array.size): IntArrayList {
            return IntArrayList(initialCapacity, initialCapacity, array.size, array)
        }
    }
}

/* todo: implement more variants */