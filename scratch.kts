import java.util.regex.Regex

fun extractStringField(json: String, field: String): String =
    Regex(""""$field"\s*:\s*"([^"]*)"|"$field"\s*:\s*(\w+)|"$field"\s*:\s*\[([^\]]*)\]""").find(json)?.let { 
        it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3].replace("\"", "").trim() }
    } ?: ""

val json = """{"id": "test_id", "role": ["conductor"], "active": true, "userid": ""}"""
println("id: '" + extractStringField(json, "id") + "'")
println("role: '" + extractStringField(json, "role") + "'")
println("active: '" + extractStringField(json, "active") + "'")
println("userid: '" + extractStringField(json, "userid") + "'")
