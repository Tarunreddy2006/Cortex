package com.cortex.app.bridge

data class BridgeResponse(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null
) {
    fun toJsonString(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"success\":").append(success)
        sb.append(",\"data\":")
        when (data) {
            null -> sb.append("null")
            is Number, is Boolean -> sb.append(data)
            is String -> {
                val str = data.trim()
                if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                    sb.append(str)
                } else {
                    sb.append("\"").append(escapeJson(str)).append("\"")
                }
            }
            else -> sb.append("\"").append(escapeJson(data.toString())).append("\"")
        }
        sb.append(",\"error\":")
        if (error == null) {
            sb.append("null")
        } else {
            sb.append("\"").append(escapeJson(error)).append("\"")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        fun ok(data: Any? = null) = BridgeResponse(success = true, data = data)
        fun err(message: String) = BridgeResponse(success = false, error = message)
    }
}
