// WITH_RUNTIME
// WITH_COROUTINES
// MODULE: controller
// FILE: controller.kt
package lib

import kotlin.coroutines.*

@AllowSuspendExtensions
class Controller {
    suspend fun String.suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume(this)
        SUSPENDED
    }

    inline suspend fun String.inlineSuspendHere(): String = suspendHere()
}

suspend fun Controller.suspendExtension(v: String): String = v.suspendHere()

inline suspend fun Controller.inlineSuspendExtension(v: String) = v.inlineSuspendHere()

// MODULE: main(controller, support)
// FILE: main.kt
import lib.*
import kotlin.coroutines.*

suspend fun Controller.localSuspendExtension(v: String) = v.suspendHere()

inline suspend fun Controller.localInlineSuspendExtension(v: String) = v.inlineSuspendHere()

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        if ("56".suspendHere() != "56") throw RuntimeException("fail 1")
        if ("28".inlineSuspendHere() != "28") throw RuntimeException("fail 2")

        if (suspendExtension("123") != "123") throw RuntimeException("fail 3")
        if (inlineSuspendExtension("234") != "234") throw RuntimeException("fail 4")

        if (localSuspendExtension("9123") != "9123") throw RuntimeException("fail 5")
        if (localInlineSuspendExtension("9234") != "9234") throw RuntimeException("fail 6")

        result = "OK"
    }

    return result
}
