package com.commissioning.cctv

/** One row of the CCTV patching schedule (sheet "OVERALL"). */
data class Item(
    val sl: Int,
    val project: String,
    val zone: String,
    val location: String,
    val equipmentTag: String,
    val rackLocation: String,
    val rackName: String,
    val patchPanel: String,
    val port: String,
    val sequence: String,
    val patchPanelTag: String,
    val cameraTag: String,
    val cameraType: String,
    val objectView: String,
    val switchType: String,
    val ppTag: String,
    val swTag: String,
    val swPort: String,
    val finalTag: String,
)

/** Verification result for one item. null = not checked yet, true = Yes, false = No. */
data class Check(
    val fieldTag: Boolean? = null,
    val cableTag: Boolean? = null,
    val cableLocation: Boolean? = null,
    val comment: String = "",
    val updatedAt: Long = 0L,
    val photos: List<String> = emptyList(),
) {
    val answers: List<Boolean?> get() = listOf(fieldTag, cableTag, cableLocation)
}

enum class Status { PENDING, IN_PROGRESS, VERIFIED, ISSUE }

fun Check?.status(): Status {
    val a = this?.answers ?: return Status.PENDING
    return when {
        a.any { it == false } -> Status.ISSUE
        a.all { it == true } -> Status.VERIFIED
        a.any { it != null } -> Status.IN_PROGRESS
        else -> Status.PENDING
    }
}

enum class StatusFilter(val label: String) {
    ALL("All"), TODO("To do"), VERIFIED("Verified"), ISSUES("Issues")
}

fun StatusFilter.matches(s: Status): Boolean = when (this) {
    StatusFilter.ALL -> true
    StatusFilter.TODO -> s == Status.PENDING || s == Status.IN_PROGRESS
    StatusFilter.VERIFIED -> s == Status.VERIFIED
    StatusFilter.ISSUES -> s == Status.ISSUE
}
