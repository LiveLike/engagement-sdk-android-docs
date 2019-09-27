package com.livelike.engagementsdk.core.exceptionhelpers

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class SafeInvocationHandler(private val target: Any) : InvocationHandler {

    override fun invoke(p0: Any?, method: Method, args: Array<out Any>?): Any {
        return if (method.returnType.name == "void" || method.returnType == Unit.javaClass) {
            try {
                method.invoke(target, args)
            } catch (ex: Throwable) {
                BugsnagClient.client?.notify(ex.cause ?: ex)
                ex.cause?.printStackTrace() ?: ex.printStackTrace()
            }
        } else
            method.invoke(target, args)
    }
}

fun <T : Any> T.safeProxy(): T {

    val loader = javaClass.classLoader
    val classes = javaClass.interfaces
    return Proxy.newProxyInstance(
        loader, classes, SafeInvocationHandler(this)
    ) as T
}
