package net.sr89.haystacker.lang.parser

fun hslParseException(message: String): HslException {
    return HslParseException(message)
}

open class HslException(message: String): RuntimeException(message) {

}

private class HslParseException(message: String) : HslException(message) {

}