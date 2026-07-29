package one.yufz.xposed

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Method

// Global XposedInterface reference – set at module init
var xposedInterface: XposedInterface? = null
    private set

fun initXposedInterface(api: XposedInterface) {
    xposedInterface = api
}

// ── Call helpers (use Java reflection since XposedHelpers is no longer available) ──

fun Any.callMethod(methodName: String, vararg args: Any): Any? {
    val types = args.map { it.javaClass }.toTypedArray()
    val m = this.javaClass.getDeclaredMethod(methodName, *types).apply { isAccessible = true }
    return m.invoke(this, *args)
}

fun Any.callMethod(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any): Any? {
    val m = this.javaClass.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
    return m.invoke(this, *args)
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any): Any? {
    val types = args.map { it.javaClass }.toTypedArray()
    val m = this.getDeclaredMethod(methodName, *types).apply { isAccessible = true }
    return m.invoke(null, *args)
}

fun Class<*>.callStaticMethod(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any): Any? {
    val m = this.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
    return m.invoke(null, *args)
}

// ── Callback types ──

typealias HookAction = HookContext.() -> Unit
typealias ReplaceAction = HookContext.() -> Any?
typealias HookCallback = HookContext.() -> Unit

// ── Extension functions for hooking ──

fun Class<*>.hookMethod(methodName: String, vararg parameterTypes: Class<*>, callback: HookCallback) {
    val method = findMethodRecursive(this, methodName, *parameterTypes) ?: return
    method.doHook(callback)
}

private fun findMethodRecursive(clazz: Class<*>, name: String, vararg params: Class<*>): Method? {
    var c: Class<*> = clazz
    while (c != Any::class.java) {
        try { return c.getDeclaredMethod(name, *params) } catch (_: NoSuchMethodException) {}
        c = c.superclass ?: break
    }
    return null
}

fun Class<*>.hookConstructor(vararg parameterTypes: Class<*>, callback: HookCallback) {
    val ctor = this.getDeclaredConstructor(*parameterTypes)
    ctor.doHook(callback)
}

fun Class<*>.hookAllConstructor(callback: HookCallback) {
    for (ctor in this.declaredConstructors) {
        ctor.doHook(callback)
    }
}

fun Class<*>.hookAllMethods(methodName: String, callback: HookCallback) {
    for (method in this.declaredMethods) {
        if (method.name == methodName) {
            method.doHook(callback)
        }
    }
}

fun Method.hook(callback: HookCallback) = this.doHook(callback)

// ── Internal hook dispatch ──

private fun Executable.doHook(callback: HookCallback) {
    val api = xposedInterface ?: return
    val ctx = HookContext()
    ctx.callback = callback
    try {
        api.hook(this).intercept(HookerImpl(ctx))
    } catch (_: Throwable) {
        // hook failed silently
    }
}

private class HookerImpl(private val ctx: HookContext) : XposedInterface.Hooker {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        ctx.chain = chain
        ctx.thisObject = chain.thisObject
        ctx.args = ArrayList(chain.args)
        ctx.result = null
        ctx.throwable = null

        // Run the user's callback (which calls doBefore/doAfter/replace)
        ctx.callback?.invoke(ctx)

        // If replace action was set, return its value
        if (ctx.replaceAction != null) {
            return ctx.replaceAction!!.invoke(ctx)
        }

        // Run before action if set
        ctx.beforeAction?.invoke(ctx)

        // Proceed
        return try {
            val result = chain.proceed()
            ctx.result = result
            ctx
        } catch (t: Throwable) {
            ctx.throwable = t
            throw t
        }.also {
            ctx.afterAction?.invoke(ctx)
        }.let {
            // If afterAction modified result, use that
            ctx.result
        }
    }
}

// ── HookContext – mimics the old XC_MethodHook.MethodHookParam ──

class HookContext {
    internal var callback: HookCallback? = null
    internal var chain: XposedInterface.Chain? = null

    var thisObject: Any? = null
        internal set

    var args: MutableList<Any?> = mutableListOf()
        internal set

    var result: Any? = null
        internal set

    var throwable: Throwable? = null
        internal set

    internal var beforeAction: HookAction? = null
        private set
    internal var afterAction: HookAction? = null
        private set
    internal var replaceAction: ReplaceAction? = null
        private set

    fun doBefore(action: HookAction) {
        this.beforeAction = action
    }

    fun doAfter(action: HookAction) {
        this.afterAction = action
    }

    fun replace(action: ReplaceAction) {
        this.replaceAction = action
    }

    val method: Executable?
        get() = chain?.executable

    fun unhook() {
        // In the new API, we'd need the HookHandle to unhook.
        // For simplicity we don't support unhooking in this compatibility layer.
    }
}

// ── ClassLoader helper ──

fun ClassLoader.findClass(className: String): Class<*> {
    return Class.forName(className, false, this)
}

// ── Other helpers that used XposedHelpers ──

fun Class<*>.newInstance(vararg args: Any): Any {
    val types = args.map { it.javaClass }.toTypedArray()
    val ctor = this.getDeclaredConstructor(*types).apply { isAccessible = true }
    return ctor.newInstance(*args)
}

fun Class<*>.newInstance(parameterTypes: Array<Class<*>>, vararg args: Any): Any {
    val ctor = this.getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }
    return ctor.newInstance(*args)
}

inline operator fun <reified T> Any.set(name: String, value: T?) {
    val field = this.javaClass.getDeclaredField(name).apply { isAccessible = true }
    field.set(this, value)
}

inline operator fun <reified T> Any.get(name: String): T {
    val field = this.javaClass.getDeclaredField(name).apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as T
}

fun Method.deoptimizeMethod() {
    // Not supported in new API – no-op
}

// XposedHelpers replacement helpers
fun Any.findMethodExact(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method {
    return clazz.getDeclaredMethod(methodName, *paramTypes).apply { isAccessible = true }
}

object XposedHelpersCompat {
    class InvocationTargetError(cause: Throwable) : Exception(cause)
}

fun findMethodExact(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method {
    return clazz.getDeclaredMethod(methodName, *paramTypes).apply { isAccessible = true }
}
