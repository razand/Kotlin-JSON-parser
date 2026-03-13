import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.KParameter

fun classFieldTypes(clazz: KClass<*>): Map<String, KType> {
    return clazz.memberProperties.associate { prop ->
        prop.name to prop.returnType
    }
}

fun parseRawJson(json: String): JSONObject = JSONObject(json)

fun bigramScore(a: String, b: String): Double {
    fun bigrams(s: String): Set<String> {
        val norm = normalizeKey(s)
        return (0 until norm.length - 1).map { norm.substring(it, it + 2) }.toSet()
    }

    val ba = bigrams(a)
    val bb = bigrams(b)
    if (ba.isEmpty() || bb.isEmpty()) return 0.0
    return ba.intersect(bb).size.toDouble() / maxOf(ba.size, bb.size)
}

fun jsonToRawMap(json: JSONObject): Map<String, String> {
    return json.keys().asSequence().associateWith { key -> json.get(key).toString() }
}

fun normalizeKey(key: String): String =
    key.lowercase().replace(Regex("[^a-z0-9]"), "")

fun coerce(value: String, type: KType): Any? {
    if (value == "null" || (type.isMarkedNullable && value.isEmpty())) return null
    return when (type.classifier) {
        String::class  -> value
        Boolean::class -> value.lowercase().let { it == "true" || it == "1" || it == "yes" }
        Int::class     -> value.lowercase().let {
            when (it) {
                "true", "yes" -> 1
                "false", "no" -> 0
                else -> it.toInt()
            }
        }
        Long::class    -> value.toLong()
        Double::class  -> value.toDouble()
        Float::class   -> value.toFloat()
        else           -> error("unsupported type: ${type.classifier}")
    }
}

fun greedyMatch(
    jsonKeys: List<String>,
    fieldNames: List<String>,
    threshold: Double = 0.3
): Map<String, String> { // fieldName -> jsonKey

    val scores = fieldNames.flatMap { field ->
        jsonKeys.map { key ->
            Triple(field, key, bigramScore(field, key))
        }
    }.sortedByDescending { it.third }

    val usedFields = mutableSetOf<String>()
    val usedKeys = mutableSetOf<String>()
    val result = mutableMapOf<String, String>()

    for ((field, key, score) in scores) {
        if (score < threshold) break
        if (field in usedFields || key in usedKeys) continue
        result[field] = key
        usedFields += field
        usedKeys += key
    }

    return result
}
fun <T : Any> construct(clazz: KClass<T>, values: Map<String, Any?>): T {
    val constructor = clazz.primaryConstructor
        ?: error("no primary constructor for ${clazz.simpleName}")
    val args = mutableMapOf<KParameter, Any?>()
    for (param in constructor.parameters) {
        when {
            values.containsKey(param.name) -> args[param] = values[param.name]
            param.isOptional -> { }
            else -> error("missing value for ${param.name}")
        }
    }
    return constructor.callBy(args)
}

// entry point
inline fun <reified T : Any> String.parseTo(): T {
    val json = parseRawJson(this)
    val rawMap = jsonToRawMap(json)
    val fieldTypes = classFieldTypes(T::class)


    val matched = greedyMatch(
        jsonKeys = rawMap.keys.toList(),
        fieldNames = fieldTypes.keys.toList()
    )

    val values = matched.mapValues { (fieldName, jsonKey) ->
        coerce(rawMap[jsonKey]!!, fieldTypes[fieldName]!!)
    }

    return construct(T::class, values)
}

fun JSONArray.toRawList(): List<JSONObject> =
    (0 until length()).map { getJSONObject(it) }
fun JSONObject.getArrayOrNull(key: String): JSONArray? =
    if (has(key)) getJSONArray(key) else null
