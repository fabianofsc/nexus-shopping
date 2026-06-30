package com.nexus.shopping.shared

inline fun <reified T : Enum<T>> requireValidEnum(
    value: String,
    fieldName: String,
    exception: (String) -> Exception,
) {
    val names = enumValues<T>().map { it.name }
    if (value !in names) throw exception("$fieldName must be one of: ${names.joinToString(", ")}.")
}
