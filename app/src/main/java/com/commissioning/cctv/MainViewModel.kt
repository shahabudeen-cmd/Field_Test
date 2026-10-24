package com.commissioning.cctv

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val items: List<Item> = Repository.loadItems(app)
    val racks: List<String> = items.map { it.rackName }.distinct()
    val results = mutableStateMapOf<Int, Check>()

    var engineer by mutableStateOf("")
        private set

    // List filters
    var query by mutableStateOf("")
    var rack by mutableStateOf<String?>(null)
    var statusFilter by mutableStateOf(StatusFilter.ALL)

    // Detail screen state: the list of SL numbers being stepped through, and the current one
    var navList by mutableStateOf<List<Int>>(emptyList())
        private set
    var selectedSl by mutableStateOf<Int?>(null)
        private set

    private val bySl = items.associateBy { it.sl }
    private var saveJob: Job? = null

    init {
        val saved = Repository.loadResults(app)
        engineer = saved.engineer
        results.putAll(saved.results)
    }

    fun item(sl: Int): Item? = bySl[sl]

    fun filtered(): List<Item> {
        val q = query.trim().lowercase()
        return items.filter { i ->
            (rack == null || i.rackName == rack) &&
                statusFilter.matches(results[i.sl].status()) &&
                (q.isEmpty() || searchText(i).contains(q))
        }
    }

    private fun searchText(i: Item): String =
        "${i.sl} ${i.cameraTag} ${i.location} ${i.rackName} ${i.patchPanel} ${i.port} ${i.sequence} ${i.finalTag}".lowercase()

    // ---- navigation ----
    fun open(sl: Int, among: List<Int>) {
        navList = among
        selectedSl = sl
    }

    fun close() {
        selectedSl = null
    }

    fun step(delta: Int) {
        val idx = navList.indexOf(selectedSl)
        val next = idx + delta
        if (idx >= 0 && next in navList.indices) selectedSl = navList[next]
    }

    // ---- editing ----
    private fun update(sl: Int, change: (Check) -> Check) {
        results[sl] = change(results[sl] ?: Check()).copy(updatedAt = System.currentTimeMillis())
        scheduleSave()
    }

    fun setFieldTag(sl: Int, v: Boolean?) = update(sl) { it.copy(fieldTag = v) }
    fun setCableTag(sl: Int, v: Boolean?) = update(sl) { it.copy(cableTag = v) }
    fun setCableLocation(sl: Int, v: Boolean?) = update(sl) { it.copy(cableLocation = v) }
    fun setComment(sl: Int, v: String) = update(sl) { it.copy(comment = v) }

    fun setEngineer(name: String) {
        engineer = name.trim()
        scheduleSave()
    }

    fun resetAll() {
        results.clear()
        scheduleSave()
        viewModelScope.launch(Dispatchers.IO) {
            Photos.dir(getApplication()).listFiles()?.forEach { it.delete() }
        }
    }

    // ---- photos ----
    private var pendingShot: File? = null
    private var pendingSl: Int = -1

    fun photoFile(name: String): File = File(Photos.dir(getApplication()), name)

    /** Temp file the camera writes into. The caller turns it into a FileProvider uri. */
    fun prepareShot(sl: Int): File {
        val f = File(Photos.shotsDir(getApplication()), "shot_${System.currentTimeMillis()}.jpg")
        pendingShot = f
        pendingSl = sl
        return f
    }

    fun finishShot(ok: Boolean) {
        val f = pendingShot ?: return
        val sl = pendingSl
        pendingShot = null
        if (!ok) { f.delete(); return }
        addPhotoFrom(sl, { f.inputStream() }, { f.delete() })
    }

    fun addPhotos(sl: Int, uris: List<Uri>) {
        val cr = getApplication<Application>().contentResolver
        uris.forEach { uri -> addPhotoFrom(sl, { cr.openInputStream(uri) }) }
    }

    private fun addPhotoFrom(sl: Int, open: () -> java.io.InputStream?, cleanup: (() -> Unit)? = null) {
        viewModelScope.launch {
            val name = "p_${System.currentTimeMillis()}_${(0..9999).random()}.jpg"
            val ok = withContext(Dispatchers.IO) { Photos.process(open, photoFile(name)).also { cleanup?.invoke() } }
            if (ok) update(sl) { it.copy(photos = it.photos + name) }
        }
    }

    fun removePhoto(sl: Int, name: String) {
        update(sl) { it.copy(photos = it.photos - name) }
        viewModelScope.launch(Dispatchers.IO) { photoFile(name).delete() }
    }

    fun photoCount(): Int = results.values.sumOf { it.photos.size }

    /** Writes every photo into a zip, named SL<no>_<n>.jpg to match the "Photo Files" report column. */
    fun exportPhotos(uri: Uri): Boolean = try {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { os ->
            ZipOutputStream(os).use { zip ->
                for ((sl, c) in results.toSortedMap()) {
                    c.photos.forEachIndexed { idx, name ->
                        val f = photoFile(name)
                        if (f.exists()) {
                            zip.putNextEntry(ZipEntry(Repository.photoName(sl, idx)))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        } != null
    } catch (e: Exception) {
        false
    }

    // ---- persistence ----
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        val snapshot = HashMap(results)
        val name = engineer
        val ctx = getApplication<Application>()
        kotlinx.coroutines.withContext(Dispatchers.IO) { Repository.saveResults(ctx, name, snapshot) }
    }

    /** Called when the app goes to background so nothing typed is lost. */
    fun flush() {
        saveJob?.cancel()
        Repository.saveResults(getApplication(), engineer, HashMap(results))
    }

    // ---- stats ----
    fun count(s: Status): Int = items.count { results[it.sl].status() == s }

    // ---- export ----
    fun export(uri: Uri): Boolean = try {
        val csv = Repository.buildCsv(items, results, engineer)
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(csv.toByteArray(Charsets.UTF_8))
        } != null
    } catch (e: Exception) {
        false
    }
}
