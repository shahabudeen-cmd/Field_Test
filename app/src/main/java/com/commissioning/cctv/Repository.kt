package com.commissioning.cctv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Repository {
    private const val ASSET = "patching_schedule.json"
    private const val RESULTS_FILE = "results.json"

    fun loadItems(context: Context): List<Item> {
        val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Item(
                sl = o.getInt("sl"),
                project = o.optString("project"),
                zone = o.optString("zone"),
                location = o.optString("location"),
                equipmentTag = o.optString("equipmentTag"),
                rackLocation = o.optString("rackLocation"),
                rackName = o.optString("rackName"),
                patchPanel = o.optString("patchPanel"),
                port = o.optString("port"),
                sequence = o.optString("sequence"),
                patchPanelTag = o.optString("patchPanelTag"),
                cameraTag = o.optString("cameraTag"),
                cameraType = o.optString("cameraType"),
                objectView = o.optString("objectView"),
                switchType = o.optString("switchType"),
                ppTag = o.optString("ppTag"),
                swTag = o.optString("swTag"),
                swPort = o.optString("swPort"),
                finalTag = o.optString("finalTag"),
            )
        }
    }

    class Saved(val engineer: String, val results: Map<Int, Check>)

    fun loadResults(context: Context): Saved {
        val f = File(context.filesDir, RESULTS_FILE)
        if (!f.exists()) return Saved("", emptyMap())
        return try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            val res = root.optJSONObject("results") ?: JSONObject()
            val map = HashMap<Int, Check>()
            for (key in res.keys()) {
                val o = res.getJSONObject(key)
                map[key.toInt()] = Check(
                    fieldTag = o.optNullableBool("f"),
                    cableTag = o.optNullableBool("c"),
                    cableLocation = o.optNullableBool("l"),
                    comment = o.optString("m"),
                    updatedAt = o.optLong("t"),
                    photos = o.optJSONArray("p")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
                )
            }
            Saved(root.optString("engineer"), map)
        } catch (e: Exception) {
            Saved("", emptyMap())
        }
    }

    @Synchronized
    fun saveResults(context: Context, engineer: String, results: Map<Int, Check>) {
        val res = JSONObject()
        for ((sl, c) in results) {
            val o = JSONObject()
            o.put("f", c.fieldTag ?: JSONObject.NULL)
            o.put("c", c.cableTag ?: JSONObject.NULL)
            o.put("l", c.cableLocation ?: JSONObject.NULL)
            o.put("m", c.comment)
            o.put("t", c.updatedAt)
            o.put("p", JSONArray(c.photos))
            res.put(sl.toString(), o)
        }
        val root = JSONObject().put("engineer", engineer).put("results", res)
        val tmp = File(context.filesDir, "$RESULTS_FILE.tmp")
        tmp.writeText(root.toString(), Charsets.UTF_8)
        tmp.renameTo(File(context.filesDir, RESULTS_FILE))
    }

    private fun JSONObject.optNullableBool(key: String): Boolean? =
        if (!has(key) || isNull(key)) null else getBoolean(key)

    // ---------- CSV export (opens directly in Excel) ----------

    private val header = listOf(
        "SL No.", "Project No.", "Zone", "Location", "Equipment Tag", "Rack Location", "Rack Name",
        "Patch Panel No.", "Port No.", "Sequence No (Recorder)", "Patchpanel Tag", "Camera Tag",
        "Camera Type", "Object View", "Switch Type", "PP Tag", "SW Tag", "SW Port No", "Final Tag",
        "Field Tag OK", "Cable Tag OK", "Cable Location OK", "Comments", "Checked By", "Checked At", "Photos", "Photo Files",
    )

    fun buildCsv(items: List<Item>, results: Map<Int, Check>, engineer: String): String {
        val sb = StringBuilder()
        sb.append('﻿') // BOM so Excel reads UTF-8 correctly
        sb.append(header.joinToString(",") { esc(it) }).append("\r\n")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (i in items) {
            val c = results[i.sl]
            val answered = c != null && c.answers.any { it != null }
            fun yn(b: Boolean?) = when (b) { true -> "Yes"; false -> "No"; null -> "" }
            val row = listOf(
                i.sl.toString(), i.project, i.zone, i.location, i.equipmentTag, i.rackLocation, i.rackName,
                i.patchPanel, i.port, i.sequence, i.patchPanelTag, i.cameraTag, i.cameraType, i.objectView,
                i.switchType, i.ppTag, i.swTag, i.swPort, i.finalTag,
                yn(c?.fieldTag), yn(c?.cableTag), yn(c?.cableLocation),
                c?.comment ?: "",
                if (answered) engineer else "",
                if (answered && c!!.updatedAt > 0) df.format(Date(c.updatedAt)) else "",
                if (c != null && c.photos.isNotEmpty()) c.photos.size.toString() else "",
                c?.photos?.indices?.joinToString(" ") { photoName(i.sl, it) } ?: "",
            )
            sb.append(row.joinToString(",") { esc(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun photoName(sl: Int, index: Int) = "SL${sl}_${index + 1}.jpg"

    private fun esc(s: String): String {
        // Prefix formula-looking text so Excel doesn't evaluate it.
        val safe = if (s.isNotEmpty() && s[0] in "=+@") "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
