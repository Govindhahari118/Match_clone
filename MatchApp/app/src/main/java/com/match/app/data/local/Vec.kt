package com.match.app.data.local

object Vec {
    fun encode(v: FloatArray): String = v.joinToString(",")
    fun decode(s: String): FloatArray =
        if (s.isBlank()) FloatArray(0)
        else s.split(',').map { it.toFloat() }.toFloatArray()
}
