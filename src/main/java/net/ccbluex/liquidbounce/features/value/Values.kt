package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationHelper
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.client.gui.FontRenderer
import java.awt.Color
import java.util.*
import kotlin.jvm.internal.Intrinsics
import kotlin.math.roundToInt

abstract class Value<T>(val name: String, var value: T) {
    val default = value
    var textHovered: Boolean = false

    private var displayableFunc: () -> Boolean = { true }

    fun displayable(func: () -> Boolean): Value<T> {
        displayableFunc = func
        return this
    }

    val displayable: Boolean
        get() = displayableFunc()

    val displayableFunction: () -> Boolean
        get() = displayableFunc

    fun set(newValue: T) {
        if (newValue == value) return

        val oldValue = get()

        try {
            onChange(oldValue, newValue)
            changeValue(newValue)
            onChanged(oldValue, newValue)
            FDPClient.configManager.smartSave()
        } catch (e: Exception) {
            ClientUtils.logError("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
        }
    }



    fun get() = value

    fun setDefault() {
        value = default
    }

    open fun changeValue(value: T) {
        this.value = value
    }

    abstract fun toJson(): JsonElement?
    abstract fun fromJson(element: JsonElement)

    protected open fun onChange(oldValue: T, newValue: T) {}
    protected open fun onChanged(oldValue: T, newValue: T) {}

    // this is better api for ListValue and TextValue

    open class ColorValue(name: String, value: Int, canDisplay: () -> Boolean) : Value<Int>(name, value) {
        val minimum: Int = -10000000
        val maximum: Int = 1000000
        fun set(newValue: Number) {
            set(newValue.toInt())
        }
        override fun toJson() = JsonPrimitive(value)
        override fun fromJson(element: JsonElement) {
            if (element.isJsonPrimitive)
                value = element.asInt
        }
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (value is String && other is String) {
            return (value as String).equals(other, true)
        }
        return value?.equals(other) ?: false
    }

    fun contains(text: String/*, ignoreCase: Boolean*/): Boolean {
        return if (value is String) {
            (value as String).contains(text, true)
        } else {
            false
        }
    }

    private var Expanded = false

    open fun getExpanded(): Boolean {
        return Expanded
    }

    open fun setExpanded(b: Boolean) {
        this.Expanded
    }

    open fun isExpanded(): Boolean {
        return Expanded
    }


    open fun getAwtColor(): Color {
        return Color((this as Value<Number>).value.toInt(), true)
    }

    open fun ColorValue(name: String, value: Int) {
        Intrinsics.checkParameterIsNotNull(name, "name")
        ColorValue(name, value)
    }
}

/**
 * Text value represents a value with a string
 */
open class TextValue(name: String, value: String) : Value<String>(name, value) {
    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asString
        }

    }
    fun append(o: Any): TextValue {
        set(get() + o)
        return this
    }

}

open class NumberValue(name: String, value: Double, val minimum: Double = 0.0, val maximum: Double = Double.MAX_VALUE,val inc: Double/* = 1.0*/)
    : Value<Double>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toDouble())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive)
            value = element.asDouble
    }
    open fun getDouble(): Double {
        return ((this.get() as Number).toDouble() / this.inc).roundToInt() * this.inc
    }

    fun append(o: Double): NumberValue {
        set(get() + o)
        return this
    }
}

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, value: String) : Value<String>(name, value) {
    @JvmField
    var openList = false

    @JvmField
    var isShown = false

    var anim=0

    @JvmField
    var open=true

    init {
        this.value = value
    }

    fun listtoggle(){
        openList=!openList;
    }

    fun getModeListNumber(mode: String) = values.indexOf(mode)
    init {
        this.value = value
    }

    fun containsValue(string: String): Boolean {
        return Arrays.stream(values).anyMatch { it.equals(string, ignoreCase = true) }
    }

    override fun changeValue(value: String) {
        for (element in values) {
            if (element.equals(value, ignoreCase = true)) {
                this.value = element
                break
            }
        }
    }

    open fun getModes() : List<String> {
        return this.values.toList()
    }

    open fun getModeGet(i: Int): String {
        return values[i]
    }

    fun isMode(string: String): Boolean {
        return this.value.equals(string, ignoreCase = true)
    }

    fun indexOf(mode: String): Int {
        for (i in values.indices) {
            if (values[i].equals(mode, true)) return i
        }
        return 0
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) changeValue(element.asString)
    }
}

/**
 * Integer value represents a value with a integer
 */
//open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE) : Value<Int>(name, value) {

open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE, val suffix: String, displayable: () -> Boolean)
    : Value<Int>(name, value) {

    constructor(name: String, value: Int, minimum: Int, maximum: Int, displayable: () -> Boolean): this(name, value, minimum, maximum, "", displayable)
    constructor(name: String, value: Int, minimum: Int, maximum: Int, suffix: String): this(name, value, minimum, maximum, suffix, { true } )
    constructor(name: String, value: Int, minimum: Int, maximum: Int): this(name, value, minimum, maximum, { true } )



    fun set(newValue: Number) {
        set(newValue.toInt())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asInt
        }
    }
}

/**
 * Block value represents a value with a block
 */
class BlockValue(name: String, value: Int) : IntegerValue(name, value, 1, 197)

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(name: String, value: Boolean) : Value<Boolean>(name, value) {

    val animation = AnimationHelper(this)
    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asBoolean || element.asString.equals("true", ignoreCase = true)
        }
    }
    init {
        animation.animationX = if (value) 5F else -5F
    }
    open fun toggle(){
        this.value = !this.value
    }

}

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val minimum: Float = 0F, val maximum: Float = Float.MAX_VALUE, val suffix: String, displayable: () -> Boolean)
    : Value<Float>(name, value) {

    constructor(name: String, value: Float, minimum: Float, maximum: Float, displayable: () -> Boolean): this(name, value, minimum, maximum, "", displayable)
    constructor(name: String, value: Float, minimum: Float, maximum: Float, suffix: String): this(name, value, minimum, maximum, suffix, { true } )
    constructor(name: String, value: Float, minimum: Float, maximum: Float): this(name, value, minimum, maximum, { true } )
    fun set(newValue: Number) {
        set(newValue.toFloat())
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asFloat
        }
    }
}

open class  ColorValue(name : String, value: Int) : Value<Int>(name, value) {
    open fun getValue(): Int {
        return super.get()
    }

    fun set(newValue: Number) {
        set(newValue.toInt())
    }


    override fun toJson() = JsonPrimitive(getValue())

    override fun fromJson(element: JsonElement) {
        if(element.isJsonPrimitive)
            value = element.asInt
    }

    open fun getHSB(): FloatArray {
        val hsbValues = FloatArray(3)
        val saturation: Float
        val brightness: Float
        var hue: Float
        var cMax: Int = (getValue() ushr 16 and 0xFF).coerceAtLeast(getValue() ushr 8 and 0xFF)
        if (getValue() and 0xFF > cMax) cMax = getValue() and 0xFF
        var cMin: Int = (getValue() ushr 16 and 0xFF).coerceAtMost(getValue() ushr 8 and 0xFF)
        if (getValue() and 0xFF < cMin) cMin = getValue() and 0xFF
        brightness = cMax.toFloat() / 255.0f
        saturation = if (cMax != 0) (cMax - cMin).toFloat() / cMax.toFloat() else 0F
        if (saturation == 0f) {
            hue = 0f
        } else {
            val redC: Float = (cMax - (getValue() ushr 16 and 0xFF)).toFloat() / (cMax - cMin).toFloat()
            // @off
            val greenC: Float = (cMax - (getValue() ushr 8 and 0xFF)).toFloat() / (cMax - cMin).toFloat()
            val blueC: Float = (cMax - (getValue() and 0xFF)).toFloat() / (cMax - cMin).toFloat() // @on
            hue =
                (if (getValue() ushr 16 and 0xFF == cMax) blueC - greenC else if (getValue() ushr 8 and 0xFF == cMax) 2.0f + redC - blueC else 4.0f + greenC - redC) / 6.0f
            if (hue < 0) hue += 1.0f
        }
        hsbValues[0] = hue
        hsbValues[1] = saturation
        hsbValues[2] = brightness
        return hsbValues
    }


}

class FontValue(valueName: String, value: FontRenderer) : Value<FontRenderer>(valueName, value) {

    private val cache: MutableList<Pair<String, FontRenderer>> = mutableListOf()
    private fun updateCache() {
        cache.clear()
        for (fontOfFonts in Fonts.getFonts()) {
            val details = Fonts.getFontDetails(fontOfFonts) ?: continue
            val name = details[0].toString()
            val size = details[1].toString().toInt()
            val format = "$name $size"

            cache.add(format to fontOfFonts)
        }

        cache.sortBy { it.first }
    }
    private fun getAllFontDetails(): Array<Pair<String, FontRenderer>> {
        if (cache.size == 0) updateCache()

        return cache.toTypedArray()
    }
    override fun toJson(): JsonElement {
        val fontDetails = Fonts.getFontDetails(value)
        val valueObject = JsonObject()
        valueObject.addProperty("fontName", fontDetails[0] as String)
        valueObject.addProperty("fontSize", fontDetails[1] as Int)
        return valueObject
    }

    override fun fromJson(element: JsonElement) {
        if (!element.isJsonObject) return
        val valueObject = element.asJsonObject
        value = Fonts.getFontRenderer(valueObject["fontName"].asString, valueObject["fontSize"].asInt)
    }

    fun set(name: String): Boolean {
        if (name.equals("Minecraft", true)) {
            set(Fonts.minecraftFont)
            return true
        } else if (name.contains(" - ")) {
            val spiced = name.split(" - ")
            set(Fonts.getFontRenderer(spiced[0], spiced[1].toInt()) ?: return false)
            return true
        }
        return false
    }
    val values
        get() = getAllFontDetails().map { it.second }

    fun setByName(name: String) {
        set((getAllFontDetails().find { it.first.equals(name, true)} ?: return).second )
    }
}