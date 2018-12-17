package utils

import kotlinx.cinterop.pin
import kotlinx.cinterop.toKString
import platform.posix.posix_errno
import platform.posix.putenv

/* python like functions */
operator fun String.times(times: Int): String {
    val sb = StringBuilder(this.length * times)
    repeat(times) {
        sb.append(this)
    }
    return sb.toString()
}

/* better getenv */
fun getenv(key: String, default: String? = null): String? {
    val ptr = platform.posix.getenv(key)
    return ptr?.toKString() ?: default
}

/* .env loader */
fun loadDotEnv(envLines: Array<String>) {
    envLines.forEach {
        val pinned = it.trim().pin() /* this should prevent from value being removed or moved */
        if (putenv(pinned.get()) != 0) {
            throw RuntimeException("Cannot putenv() errno=${posix_errno()}")
        }
    }
}

/* better errno */
fun errnoReadable(): String {
    return when (posix_errno()) {
        1 -> "EPERM        1   Operation not permitted"
        2 -> "ENOENT       2   No such file or directory"
        3 -> "ESRCH        3   No such process"
        4 -> "EINTR        4   Interrupted system call"
        5 -> "EIO          5   I/O error"
        6 -> "ENXIO        6   No such device or address"
        7 -> "E2BIG        7   Argument list too long"
        8 -> "ENOEXEC      8   Exec format error"
        9 -> "EBADF        9   Bad file number"
        10 -> "ECHILD      10   No child processes"
        11 -> "EAGAIN      11   Try again"
        12 -> "ENOMEM      12   Out of memory"
        13 -> "EACCES      13   Permission denied"
        14 -> "EFAULT      14   Bad address"
        15 -> "ENOTBLK     15   Block device required"
        16 -> "EBUSY       16   Device or resource busy"
        17 -> "EEXIST      17   File exists"
        18 -> "EXDEV       18   Cross-device link"
        19 -> "ENODEV      19   No such device"
        20 -> "ENOTDIR     20   Not a directory"
        21 -> "EISDIR      21   Is a directory"
        22 -> "EINVAL      22   Invalid argument"
        23 -> "ENFILE      23   File table overflow"
        24 -> "EMFILE      24   Too many open files"
        25 -> "ENOTTY      25   Not a typewriter"
        26 -> "ETXTBSY     26   Text file busy"
        27 -> "EFBIG       27   File too large"
        28 -> "ENOSPC      28   No space left on device"
        29 -> "ESPIPE      29   Illegal seek"
        30 -> "EROFS       30   Read-only file system"
        31 -> "EMLINK      31   Too many links"
        32 -> "EPIPE       32   Broken pipe"
        33 -> "EDOM        33   Math argument out of domain of func"
        34 -> "ERANGE      34   Math result not representable"
        35 -> "EDEADLK     35   Resource deadlock would occur"
        36 -> "ENAMETOOLONG    36   File name too long"
        37 -> "ENOLCK      37   No record locks available"
        38 -> "ENOSYS      38   Function not implemented"
        39 -> "ENOTEMPTY   39   Directory not empty"
        40 -> "ELOOP       40   Too many symbolic links encountered"
        41 -> "EWOULDBLOCK  41   Operation would block"
        42 -> "ENOMSG      42   No message of desired type"
        43 -> "EIDRM       43   Identifier removed"
        44 -> "ECHRNG      44   Channel number out of range"
        45 -> "EL2NSYNC    45   Level 2 not synchronized"
        46 -> "EL3HLT      46   Level 3 halted"
        47 -> "EL3RST      47   Level 3 reset"
        48 -> "ELNRNG      48   Link number out of range"
        49 -> "EUNATCH     49   Protocol driver not attached"
        50 -> "ENOCSI      50   No CSI structure available"
        51 -> "EL2HLT      51   Level 2 halted"
        52 -> "EBADE       52   Invalid exchange"
        53 -> "EBADR       53   Invalid request descriptor"
        54 -> "EXFULL      54   Exchange full"
        55 -> "ENOANO      55   No anode"
        56 -> "EBADRQC     56   Invalid request code"
        57 -> "EBADSLT     57   Invalid slot"
        58 -> "EDEADLOCK   58   Deadlock"
        59 -> "EBFONT      59   Bad font file format"
        60 -> "ENOSTR      60   Device not a stream"
        61 -> "ENODATA     61   No data available"
        62 -> "ETIME       62   Timer expired"
        63 -> "ENOSR       63   Out of streams resources"
        64 -> "ENONET      64   Machine is not on the network"
        65 -> "ENOPKG      65   Package not installed"
        66 -> "EREMOTE     66   Object is remote"
        67 -> "ENOLINK     67   Link has been severed"
        68 -> "EADV        68   Advertise error"
        69 -> "ESRMNT      69   Srmount error"
        70 -> "ECOMM       70   Communication error on send"
        71 -> "EPROTO      71   Protocol error"
        72 -> "EMULTIHOP   72   Multihop attempted"
        73 -> "EDOTDOT     73   RFS specific error"
        74 -> "EBADMSG     74   Not a data message"
        75 -> "EOVERFLOW   75   Value too large for defined data type"
        76 -> "ENOTUNIQ    76   Name not unique on network"
        77 -> "EBADFD      77   File descriptor in bad state"
        78 -> "EREMCHG     78   Remote address changed"
        79 -> "ELIBACC     79   Can not access a needed shared library"
        80 -> "ELIBBAD     80   Accessing a corrupted shared library"
        81 -> "ELIBSCN     81   .lib section in a.out corrupted"
        82 -> "ELIBMAX     82   Attempting to link in too many shared libraries"
        83 -> "ELIBEXEC    83   Cannot exec a shared library directly"
        84 -> "EILSEQ      84   Illegal byte sequence"
        85 -> "ERESTART    85   Interrupted system call should be restarted"
        86 -> "ESTRPIPE    86   Streams pipe error"
        87 -> "EUSERS      87   Too many users"
        88 -> "ENOTSOCK    88   Socket operation on non-socket"
        89 -> "EDESTADDRREQ    89   Destination address required"
        90 -> "EMSGSIZE    90   Message too long"
        91 -> "EPROTOTYPE  91   Protocol wrong type for socket"
        92 -> "ENOPROTOOPT 92   Protocol not available"
        93 -> "EPROTONOSUPPORT 93   Protocol not supported"
        94 -> "ESOCKTNOSUPPORT 94   Socket type not supported"
        95 -> "EOPNOTSUPP  95   Operation not supported on transport endpoint"
        96 -> "EPFNOSUPPORT    96   Protocol family not supported"
        97 -> "EAFNOSUPPORT    97   Address family not supported by protocol"
        98 -> "EADDRINUSE  98   Address already in use"
        99 -> "EADDRNOTAVAIL   99   Cannot assign requested address"
        100 -> "ENETDOWN    100  Network is down"
        101 -> "ENETUNREACH 101  Network is unreachable"
        102 -> "ENETRESET   102  Network dropped connection because of reset"
        103 -> "ECONNABORTED    103  Software caused connection abort"
        104 -> "ECONNRESET  104  Connection reset by peer"
        105 -> "ENOBUFS     105  No buffer space available"
        106 -> "EISCONN     106  Transport endpoint is already connected"
        107 -> "ENOTCONN    107  Transport endpoint is not connected"
        108 -> "ESHUTDOWN   108  Cannot send after transport endpoint shutdown"
        109 -> "ETOOMANYREFS    109  Too many references: cannot splice"
        110 -> "ETIMEDOUT   110  Connection timed out"
        111 -> "ECONNREFUSED    111  Connection refused"
        112 -> "EHOSTDOWN   112  Host is down"
        113 -> "EHOSTUNREACH    113  No route to host"
        114 -> "EALREADY    114  Operation already in progress"
        115 -> "EINPROGRESS 115  Operation now in progress"
        116 -> "ESTALE      116  Stale NFS file handle"
        117 -> "EUCLEAN     117  Structure needs cleaning"
        118 -> "ENOTNAM     118  Not a XENIX named type file"
        119 -> "ENAVAIL     119  No XENIX semaphores available"
        120 -> "EISNAM      120  Is a named type file"
        121 -> "EREMOTEIO   121  Remote I/O error"
        122 -> "EDQUOT      122  Quota exceeded"
        123 -> "ENOMEDIUM   123  No medium found"
        124 -> "EMEDIUMTYPE 124  Wrong medium type"
        125 -> "ECANCELED   125  Operation Canceled"
        126 -> "ENOKEY      126  Required key not available"
        127 -> "EKEYEXPIRED 127  Key has expired"
        128 -> "EKEYREVOKED 128  Key has been revoked"
        129 -> "EKEYREJECTED    129  Key was rejected by service"
        else -> "Invalid errno code!!!"
    }
}