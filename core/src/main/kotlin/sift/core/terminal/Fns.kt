package sift.core.terminal

private val emojiRegex = Regex("((" +
    "[\uD83C\uDF00-\uD83D\uDDFF]|" +  // | symbols and pictographs
    "[\uD83D\uDE00-\uD83D\uDE4F]|" +  // | emoticons
    "[\uD83D\uDE80-\uD83D\uDEFF]|" +  // | transport and map symbols
    "[\u2600-\u26FF]|" +              // | miscellaneous symbols
    "[\u2700-\u27BF])" +              // ) dingbats
    "[\\x{1F3FB}-\\x{1F3FF}]?\\s?)"   // ) skin tone + opt trailing whitespace
)

fun String.stripEmoji(): String = replace(emojiRegex, "")

class Fns