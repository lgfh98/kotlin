// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// CHECK_NO_CONTINUATION: empty$1
// CHECK_NO_CONTINUATION: generic$1
// CHECK_NO_CONTINUATION: genericInferType$1
// CHECK_NO_CONTINUATION: lambdaAsParameter$1
// CHECK_NO_CONTINUATION: lambdaAsParameterReturn$1
// CHECK_NO_CONTINUATION: multipleExitPoints$1
// CHECK_NO_CONTINUATION: multipleExitPointsWhen$1
// CHECK_NO_CONTINUATION: multipleExitPointsWithOrdinaryInline$1
// CHECK_NO_CONTINUATION: nullableUnit$1
// CHECK_NO_CONTINUATION: returnsInt$1
// CHECK_NO_CONTINUATION: useGenericClass$1
// CHECK_NO_CONTINUATION: useGenericIngerType$1
// CHECK_NO_CONTINUATION: useGenericReturningUnit$1
// CHECK_NO_CONTINUATION: useRunRunRunRunRun$1
// CHECK_NO_CONTINUATION: withReturn$1
// CHECK_NO_CONTINUATION: withoutReturn$1

suspend fun empty() {}
suspend fun withoutReturn() { empty() }
suspend fun withReturn() { return empty() }
suspend fun notTailCall() { empty(); empty() }
suspend fun lambdaAsParameter(c: suspend ()->Unit) { c() }
suspend fun lambdaAsParameterNotTailCall(c: suspend ()->Unit) { c(); c() }
suspend fun lambdaAsParameterReturn(c: suspend ()->Unit) { return c() }
suspend fun returnsInt() = 42
// This should not be tail-call, since the caller should push Unit.INSTANCE on stack
suspend fun callsIntNotTailCall() { returnsInt() }
suspend fun multipleExitPoints(b: Boolean) { if (b) empty() else withoutReturn() }
suspend fun multipleExitPointsNotTailCall(b: Boolean) { if (b) empty() else returnsInt() }

fun ordinary() = 1
inline fun ordinaryInline() { ordinary() }
suspend fun multipleExitPointsWithOrdinaryInline(b: Boolean) { if (b) empty() else ordinaryInline() }

suspend fun multipleExitPointsWhen(i: Int) {
    when(i) {
        1 -> empty()
        2 -> withReturn()
        3 -> withoutReturn()
        else -> lambdaAsParameter {}
    }
}

suspend fun <T> generic(): T = TODO()
suspend fun useGenericReturningUnit() {
    generic<Unit>()
}

class Generic<T> {
    suspend fun foo(): T = TODO()
}
suspend fun useGenericClass(g: Generic<Unit>) {
    g.foo()
}

suspend fun <T> genericInferType(c: () -> T): T = TODO()
suspend fun useGenericIngerType() {
    genericInferType {}
}

suspend fun nullableUnit(): Unit? = null
suspend fun useNullableUnit() {
    nullableUnit()
}

suspend fun useRunRunRunRunRun() {
    run {
        run {
            run {
                run {
                    run {
                        empty()
                    }
                }
            }
        }
    }
}
