// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// CHECK_NO_CONTINUATION: f$1

import COROUTINES_PACKAGE.intrinsics.*

fun check() = true

suspend fun f(i: Int): Unit {
    return f_2()
}

private inline suspend fun f_2(): Unit {
    if (check()) return
    return suspendCoroutineUninterceptedOrReturn {
        COROUTINE_SUSPENDED
    }
}
