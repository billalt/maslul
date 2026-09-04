package com.maslul.assets.importing

internal inline fun <reified T : Enum<T>> parseImportEnum(raw: String, column: String): T =
    enumValues<T>().firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        ?: throw RowParseException(
            "$column '$raw' is not one of ${enumValues<T>().joinToString { it.name }}",
        )
