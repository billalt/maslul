package com.maslul.assets.importing

// Caught by each parser's per-row/per-feature loop and turned into ImportRow.parseError -
// never propagates out of ServicePointImportParser.parse().
internal class RowParseException(message: String) : Exception(message)
