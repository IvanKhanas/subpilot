package com.xeno.subpilot.chat.testcontainers

fun awaitCondition(
    timeoutMs: Long = 5000,
    pollMs: Long = 100,
    condition: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "Condition not met within ${timeoutMs}ms" }
        Thread.sleep(pollMs)
    }
}
