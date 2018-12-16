package io

import kotlinx.cinterop.cstr
import platform.posix.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class PathTests {

    @Test
    fun `extension`() {
        assertEquals("zip", Path("file.zip").extension)
        assertEquals("gz", Path("archive.tar.gz").extension)
        assertEquals("bf", Path("b3af409bb8423187c75e6c7f5b683908.bf").extension)
        assertEquals("log", Path("/var/log/app.log").extension)
        assertEquals("", Path("/etc/mtab").extension)
        assertEquals("", Path("/etc/init.d/mysqld").extension)
    }

    @Test
    fun `basename`() {
        assertEquals("file.zip", Path("file.zip").filename)
        assertEquals("archive.tar.gz", Path("archive.tar.gz").filename)
        assertEquals("b3af409bb8423187c75e6c7f5b683908.bf", Path("b3af409bb8423187c75e6c7f5b683908.bf").filename)
        assertEquals("app.log", Path("/var/log/app.log").filename)
        assertEquals("mtab", Path("/etc/mtab").filename)
        assertEquals("mysqld", Path("/etc/init.d/mysqld").filename)
        assertEquals("mysqld.sh", Path("/etc/init.d/mysqld.sh").filename)
    }

    @Test
    fun `isFile`() {
        mkdir("./test_dir")
        fclose(fopen("./test_file.txt", "w"))
        fclose(fopen("./test_file", "w"))
        assertFalse(Path("./test_dir").isFile())
        assertTrue(Path("./test_file.txt").isFile())
        assertTrue(Path("./test_file").isFile())
    }

    @Test
    fun `isDirectory`() {
        mkdir("./test_dir")
        fclose(fopen("./test_file.txt", "w"))
        fclose(fopen("./test_file", "w"))
        assertTrue(Path("./test_dir").isDirectory())
        assertTrue(Path("../").isDirectory())
        assertTrue(Path(".").isDirectory())
        assertFalse(Path("./test_file.txt").isDirectory())
        assertFalse(Path("./test_file").isDirectory())
    }

    @Test
    fun `exists`() {
        mkdir("./test_dir")
        fclose(fopen("./test_file.txt", "w"))
        fclose(fopen("./test_file", "w"))
        assertTrue(Path("./test_dir").exists())
        assertTrue(Path("../").exists())
        assertTrue(Path(".").exists())
        assertTrue(Path("./test_file.txt").exists())
        assertTrue(Path("./test_file").exists())
        assertFalse(Path("./lol").exists())
        assertFalse(Path("./non_existing").exists())
        assertFalse(Path("file.txt").exists())
    }

    @Test
    fun `size`() {
        fclose(fopen("./test_file.txt", "w"))
        val fd = fopen("./test_file10b.txt", "w")
        fwrite("0000000000".cstr, 1, 10, fd)
        fclose(fd)
        assertEquals(Path("./test_file.txt").size(), 0)
        assertEquals(Path("./test_file10b.txt").size(), 10)
    }

    @Test
    fun `relativize`() {
        assertEquals("/var/log", Path.join(Path("/var/").relativize(Path("log"))).toString())
        assertEquals("C:/Windows/test.txt", Path("C:/Windows").relativize(Path("test.txt")).toString())
        assertEquals("C:/Windows/test.txt", Path("C:/Windows").relativize(Path("/test.txt")).toString())
    }

    @Test
    fun `path join`() {
        assertEquals("/var/log/syslog", Path.join(Path("/var/"), Path("log"), Path("/syslog")).toString())
        assertEquals("C:/Windows/test.txt", Path.join(Path("C:/Windows/"), Path("/test.txt")).toString())
        assertEquals("C:/Windows/test.txt", Path.join(Path("C:/Windows"), Path("test.txt")).toString())
        assertEquals("C:/Windows/test.txt", Path.join(Path("C:/Windows"), Path("/test.txt")).toString())
        assertEquals("C:/Windows/test.txt", Path.join(Path("C:/Windows"), Path("/test.txt")).toString())
        assertEquals("./test/file.png", Path.join(Path("."), Path("test"), Path("file.png")).toString())
        assertEquals("./test/file.png", Path.join(Path("./"), Path("test"), Path("file.png")).toString())
        assertEquals("./test/file.png", Path.join(Path("./"), Path("/test/"), Path("/file.png")).toString())
    }

    @Test
    fun `path extension replacement`() {
        assertEquals("/var/log/syslog.txt", Path("/var/log/syslog.log").withExtension("txt").toString())
        assertEquals("/var/log/syslog.txt", Path("/var/log/syslog.log").withExtension(".txt").toString())
        assertEquals("file.txt", Path("file.log").withExtension("txt").toString())
        assertEquals("file.txt", Path("file.log").withExtension(".txt").toString())
        assertEquals("file.tar.gz", Path("file.log").withExtension("tar.gz").toString())
        assertEquals("file.tar.gz", Path("file.log").withExtension(".tar.gz").toString())
    }

}