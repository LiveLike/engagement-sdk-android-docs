@file:Suppress("UNCHECKED_CAST")

package com.livelike.engagementsdk.core.exceptionhelpers

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class SafeInvocationHandler(val target: Any) : InvocationHandler {

    override fun invoke(p0: Any?, method: Method, args: Array<out Any>?): Any? {
        return if (method.returnType.name == "void" || method.returnType == Unit.javaClass) {
            try {
                if (args?.isNotEmpty() == true) {
                    method.invoke(target, *args) // kotlin uses spread operator(*) to pass array as varargs
                } else {
                    method.invoke(target)
                }
            } catch (ex: Throwable) {
                BugsnagClient.client?.notify(ex.cause ?: ex)
                ex.cause?.printStackTrace() ?: ex.printStackTrace()
            }
        } else if (args?.isNotEmpty() == true) method.invoke(target, *args) else method.invoke(
            target
        )
    }
}

internal fun <T : Any> T.safeProxyForEmptyReturnCalls(): T {

    val loader = javaClass.classLoader
    val classes = javaClass.interfaces
    return Proxy.newProxyInstance(
        loader, classes, SafeInvocationHandler(this)
    ) as T
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal fun <T> T.getTargetObject(): Any? {
    val clazz = (this as Object).getClass()
    return if (Proxy.isProxyClass(clazz)) {
        (Proxy.getInvocationHandler(this) as SafeInvocationHandler).target
    } else {
        this
    }
}
