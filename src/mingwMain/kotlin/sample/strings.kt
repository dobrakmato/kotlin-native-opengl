package sample

/* Python-like functions. */

operator fun String.times(times: Int): String {
    val sb = StringBuilder(this.length * times)
    repeat(times) {
        sb.append(this)
    }
    return sb.toString()
}