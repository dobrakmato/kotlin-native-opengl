package utils

import kotlin.math.max

data class Option(val short: Char, val long: String, val description: String)
data class Value(val long: String, val description: String, val required: Boolean)

class Options {

    private val options = mutableListOf<Option>()
    internal val values = mutableListOf<Value>()

    internal val optionShortNames = mutableMapOf<Char, Option>()
    internal val optionLongNames = mutableMapOf<String, Option>()

    internal val valueLongNames = mutableMapOf<String, Value>()

    private var longestOptionLong = 0
    private var longestValueLong = 0

    fun option(short: Char, long: String, description: String): Options {
        val opt = Option(short, long, description)
        options.add(opt)
        optionShortNames[short] = opt
        optionLongNames[long] = opt
        longestOptionLong = max(longestOptionLong, long.length)
        return this
    }

    fun requiredValue(long: String, description: String): Options {
        val value = Value(long, description, true)
        values.add(value)
        valueLongNames[long] = value
        longestValueLong = max(longestValueLong, long.length)
        return this
    }

    fun optionalValue(long: String, description: String): Options {
        val value = Value(long, description, false)
        values.add(value)
        valueLongNames[long] = value
        longestValueLong = max(longestValueLong, long.length)
        return this
    }

    fun parse(args: Array<String>): ActualOptions {
        val actual = ActualOptions(this, args)
        actual.parse()
        return actual
    }

    fun displayHelp(appName: String = "app.exe") {
        fun combineAllOptionShorts(): String {
            var res = "-"
            for (option in options) res += option.short
            return res
        }

        fun combineAllRequiredValues(): String {
            var res = ""
            for (value in values) if (value.required) res += "--${value.long}= "
            return res
        }

        fun formatOption(option: Option): String {
            return "    -${option.short}      --${option.long.padEnd(
                longestOptionLong,
                ' '
            )}          : ${option.description}"
        }

        fun formatValue(value: Value): String {
            return "    --${value.long.padEnd(
                longestValueLong,
                ' '
            )}          : ${if (value.required) "(REQUIRED) " else ""}${value.description}"
        }

        println("$appName ${combineAllOptionShorts()} ${combineAllRequiredValues()}[OPTIONAL_ARGS]")
        println("\nOptions:")
        for (option in options) println(formatOption(option))
        println("\nArguments:")
        for (value in values) println(formatValue(value))
    }

    fun hasValue(longName: String): Boolean {
        if (longName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        return longName in valueLongNames
    }

    fun hasOption(shortOrLongName: String): Boolean {
        if (shortOrLongName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        return (shortOrLongName.length == 1 && shortOrLongName[0] in optionShortNames) || shortOrLongName in optionLongNames
    }
}

class ActualOptions(private val options: Options, private val cmdLine: Array<String>) {

    private val presentOptions = mutableListOf<Option>()
    private val presentValues = mutableMapOf<String, String>()

    fun isOptionPresent(shortOrLongName: String): Boolean {
        if (shortOrLongName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        for (option in presentOptions) {
            if ((shortOrLongName.length == 1 && option.short == shortOrLongName[0]) || option.long == shortOrLongName) {
                return true
            }
        }
        return false
    }

    fun isValuePresent(longName: String): Boolean {
        if (longName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        return longName in presentValues
    }

    fun getOptionalValue(longName: String, defaultValue: String): String {
        if (longName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        if (isValuePresent(longName)) {
            return presentValues[longName]!!
        }
        if (options.hasValue(longName)) {
            return defaultValue
        }
        throw IllegalArgumentException("No value with long-name $longName is defined in options!")
    }

    fun getValue(longName: String): String {
        if (longName.isEmpty()) throw IllegalArgumentException("Empty name is not valid name!")
        if (isValuePresent(longName)) {
            return presentValues[longName]!!
        }
        throw IllegalArgumentException("No value with long-name $longName is defined in options!")
    }

    fun hasAllRequiredValues(): Boolean {
        for (value in options.values) {
            if (value.required && !isValuePresent(value.long)) {
                return false
            }
        }
        return true
    }

    fun shouldDisplayHelp(): Boolean {
        if (!hasAllRequiredValues()) return true
        return (!options.hasOption("h") && isOptionPresent("h") || isOptionPresent("help"))
    }

    internal fun parse() {
        /* check each argument */
        for (e in cmdLine) {
            if (e.startsWith('-')) { // if it is opt argument
                if (e.length > 1) { // and has content
                    if (e[1] == '-') {
                        /* long-name */
                        val kv = e.substring(2)
                        if (kv.contains("=")) {
                            /* long-name value */
                            val p = kv.split('=')
                            val k = p[0]
                            val v = p[1]
                            if (k in options.valueLongNames) {
                                presentValues[k] = v
                            }
                        } else {
                            /* long-name option */
                            if (kv in options.optionLongNames) {
                                presentOptions.add(options.optionLongNames[kv]!!)
                            }
                        }
                    } else {
                        /* short-name (multi)option */
                        val opts = e.substring(1)
                        /* process all chars as possible options */
                        for (c in opts) {
                            if (c in options.optionShortNames) {
                                presentOptions.add(options.optionShortNames[c]!!)
                            }
                        }
                    }
                }
            }
        }
    }

    fun displayHelp(appName: String = "app.exe") = options.displayHelp(appName)
}