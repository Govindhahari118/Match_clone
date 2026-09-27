package com.match.app.data.repo

internal object InterestListPolicy {
    fun pendingCounterparts(counterpartUids: List<String>, mutualUids: Set<String>): List<String> =
        counterpartUids
            .asSequence()
            .filter { it.isNotBlank() && it !in mutualUids }
            .distinct()
            .toList()
}
