package com.aifusion.app.research

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ResearchStore(context: Context) {
    private val prefs = context.getSharedPreferences("research_lab", Context.MODE_PRIVATE)

    fun saveProject(project: ResearchProject) {
        val array = JSONArray(prefs.getString("projects", "[]"))
        val output = JSONArray()
        var replaced = false
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optString("id") == project.id) {
                output.put(toJson(project))
                replaced = true
            } else output.put(item)
        }
        if (!replaced) output.put(toJson(project))
        prefs.edit().putString("projects", output.toString()).apply()
    }

    fun listProjects(): List<ResearchProject> {
        val array = JSONArray(prefs.getString("projects", "[]"))
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            ResearchProject(
                id = o.optString("id"),
                title = o.optString("title"),
                query = o.optString("query"),
                status = runCatching { ResearchProjectStatus.valueOf(o.optString("status")) }.getOrDefault(ResearchProjectStatus.DRAFT),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }.sortedByDescending { it.updatedAt }
    }

    fun deleteProject(id: String) {
        val output = JSONArray()
        val array = JSONArray(prefs.getString("projects", "[]"))
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optString("id") != id) output.put(item)
        }
        prefs.edit().putString("projects", output.toString()).apply()
    }

    private fun toJson(project: ResearchProject) = JSONObject()
        .put("id", project.id)
        .put("title", project.title)
        .put("query", project.query)
        .put("status", project.status.name)
        .put("createdAt", project.createdAt)
        .put("updatedAt", project.updatedAt)
}
