package utils

import kotlin.test.*


class IntArrayListTests {

    @Test
    fun `new list has zero size`() {
        val list = IntArrayList()
        assertEquals(0, list.size)
    }

    @Test
    fun `cannot read out of bounds`() {
        val list = IntArrayList()
        assertFails {
            list[0]
        }
        assertFails {
            list[2]
        }
        assertFails {
            list[-1]
        }
        list.add(1)
        assertFails {
            list[1]
        }
    }

    @Test
    fun `add`() {
        val list = IntArrayList()
        list.add(20)
        list.add(40)
        list.add(50)
        assertEquals(3, list.size)
        assertEquals(20, list[0])
        assertEquals(40, list[1])
        assertEquals(50, list[2])
    }

    @Test
    fun `add all`() {
        val list = IntArrayList()
        list.add(20)
        list.add(40)
        val list2 = IntArrayList()
        list2.add(3)
        list2.addAll(list)
        assertEquals(3, list2.size)
        assertEquals(3, list2[0])
        assertEquals(20, list2[1])
        assertEquals(40, list2[2])
    }

    @Test
    fun `resizes itself`() {
        val list = IntArrayList(2)
        list.add(10)
        list.add(20)
        list.add(30)
        list.add(40)
        assertEquals(10, list[0])
        assertEquals(20, list[1])
        assertEquals(30, list[2])
        assertEquals(40, list[3])
    }

    @Test
    fun `clear`() {
        val list = IntArrayList()
        list.add(20)
        list.add(40)
        list.add(50)
        assertEquals(3, list.size)
        assertEquals(20, list[0])
        list.clear()
        assertEquals(0, list.size)
        assertFails {
            list[0]
        }
    }

    @Test
    fun `equals`() {
        val list = IntArrayList()
        list.add(1)
        list.add(3)
        list.add(-5)
        val list2 = IntArrayList()
        list2.add(1)
        list2.add(3)
        list2.add(-5)
        assertEquals(list, list2)
    }

    @Test
    fun `indexOf`() {
        val list = IntArrayList()
        list.add(1)
        list.add(3)
        list.add(-5)
        list.add(1)
        list.add(2)
        assertEquals(0, list.indexOf(1))
        assertEquals(1, list.indexOf(3))
        assertEquals(2, list.indexOf(-5))
        assertEquals(-1, list.indexOf(10000))
        list.clear()
        assertEquals(-1, list.indexOf(1))
    }

    @Test
    fun `contains`() {
        val list = IntArrayList()
        list.add(1)
        list.add(3)
        list.add(-5)
        list.add(1)
        list.add(2)
        assertTrue(list.contains(1))
        assertTrue(list.contains(-5))
        assertTrue(list.contains(2))
        assertFalse(list.contains(0))
        assertFalse(list.contains(6))
        assertFalse(list.contains(-1000))
        assertTrue(list.containsAll(listOf(1, 3, 2, -5)))
    }

    @Test
    fun `isEmpty`() {
        val list = IntArrayList()
        assertTrue(list.isEmpty())
        list.add(1)
        list.add(3)
        list.add(-5)
        assertFalse(list.isEmpty())
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `lastIndexOf`() {
        val list = IntArrayList()
        list.add(1) // 0
        list.add(3)  // 1
        list.add(-5) // 2
        list.add(1)  // 3
        list.add(2)  // 4
        assertEquals(3, list.lastIndexOf(1))
        assertEquals(1, list.lastIndexOf(3))
        assertEquals(2, list.lastIndexOf(-5))
        assertEquals(4, list.lastIndexOf(2))
        assertEquals(-1, list.lastIndexOf(10000))
    }

    @Test
    fun `iterator`() {
        val list = IntArrayList()
        list.add(1)
        list.add(3)
        list.add(-5)
        list.add(1)
        list.add(2)
        val iter = list.iterator()
        assertTrue(iter.hasNext())
        assertEquals(1, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(3, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(-5, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(1, iter.next())
        assertTrue(iter.hasNext())
        assertEquals(2, iter.next())
        assertFalse(iter.hasNext())
    }

    @Test
    fun `subList`() {
        val list = IntArrayList()
        list.add(1)
        list.add(3)
        list.add(-5)
        list.add(1)
        list.add(2)
        val sub = list.subList(2, 3)
        assertEquals(-5, sub[0])
        assertEquals(1, sub[1])
    }
}