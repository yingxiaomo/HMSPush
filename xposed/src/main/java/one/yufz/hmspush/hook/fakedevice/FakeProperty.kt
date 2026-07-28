package one.yufz.hmspush.hook.fakedevice

import android.os.Build
import one.yufz.hmspush.hook.XLog
import one.yufz.xposed.*
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.reflect.Modifier

private const val TAG = "FakeProperties"

enum class Property(val entry: Pair<String, String>) {
    EMUI_API("ro.build.hw_emui_api_level" to "21"),
    EMUI_VERSION("ro.build.version.emui" to "EmotionUI_8.0.0"),
    BRAND("ro.product.brand" to "Huawei"),
    MANUFACTURER("ro.product.manufacturer" to "HUAWEI"),
    MODEL("ro.product.model" to "HUAWEI P30 Pro"),
    DISPLAY("ro.build.display.id" to "EmotionUI_8.0.0"),
    MIUI_VERSION("ro.miui.ui.version.name" to "");

    val key: String
        get() = entry.first

    val value: String
        get() = entry.second
}


fun fakeProperty(property: Property, overrideValue: String) = fakeProperty(Pair(property.key, overrideValue))

fun fakeAllBuildInProperties() = fakeProperty(*Property.values().map { it.entry }.toTypedArray())

fun fakeProperty(vararg properties: Property) {
    fakeProperty(*properties.map { it.entry }.toTypedArray())
}

private val propertyMap: MutableMap<String, String> = HashMap()
private val hooked = AtomicBoolean(false)

fun fakeProperty(vararg properties: Pair<String, String>) {
    propertyMap.putAll(properties)

    if (propertyMap.containsKey(Property.BRAND.key)) {
        setFinalStatic(Build::class.java, "BRAND", propertyMap[Property.BRAND.key])
    }

    if (propertyMap.containsKey(Property.MANUFACTURER.key)) {
        setFinalStatic(Build::class.java, "MANUFACTURER", propertyMap[Property.MANUFACTURER.key])
    }

    if (propertyMap.containsKey(Property.MODEL.key)) {
        setFinalStatic(Build::class.java, "MODEL", propertyMap[Property.MODEL.key])
    }

    if (propertyMap.containsKey(Property.DISPLAY.key)) {
        setFinalStatic(Build::class.java, "DISPLAY", propertyMap[Property.DISPLAY.key])
    }

    if (propertyMap.containsKey("ro.build.user")) {
        setFinalStatic(Build::class.java, "USER", propertyMap["ro.build.user"])
    }

    if (hooked.getAndSet(true)) return

    val classSystemProperties = Build::class.java.classLoader!!.findClass("android.os.SystemProperties")

    val callback: HookContext.() -> Unit = {
        doBefore {
            val key = args[0] as String
            propertyMap[key]?.let {
                result = it
            }
        }
    }

    classSystemProperties.hookMethod("get", String::class.java, callback = callback)
    classSystemProperties.hookMethod("get", String::class.java, String::class.java, callback = callback)

    Runtime::class.java.hookMethod("exec", String::class.java) {
        doBefore {
            val cmd = args[0] as String
            if (cmd.startsWith("getprop")) {
                val key = cmd.removePrefix("getprop").trim()
                propertyMap[key]?.let {
                    XLog.d(TAG, "hook getprop $key")
                    args[0] = "echo $it"
                }
            }
        }
    }
}

private fun setFinalStatic(clazz: Class<*>, fieldName: String, value: Any?) {
    try {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true

        // Remove final modifier so we can overwrite the field
        val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("accessFlags")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        field.set(null, value)
        XLog.d(TAG, "修改 $fieldName 成功")
    } catch (t: Throwable) {
        XLog.e(TAG, "修改 $fieldName 失败", t)
    }
}