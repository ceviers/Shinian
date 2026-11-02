package com.cevier.shinian.model

enum class NoteType {
    TEXT,
    IMAGE,
    AUDIO,
}

data class Note(
    val id: Long,
    val day: String,
    val time: String,
    val type: NoteType,
    val duration: String? = null,
    val title: String,
    val body: String,
    val accent: Long,
    val mediaPath: String? = null,
    val recordedDate: String? = null,
)

/**
 * A deliberately small palette for the timeline accent. The selected value is
 * stored on each Note, so it remains stable across recomposition, app restarts,
 * edits, exports, and restores.
 */
private val NOTE_ACCENT_PALETTE = longArrayOf(
    0xFFFF8A34,
    0xFF55A4F0,
    0xFF2F6FED,
    0xFF32A47C,
    0xFF7A63D8,
    0xFFE06886,
    0xFFE0A12D,
)

fun randomNoteAccent(): Long = NOTE_ACCENT_PALETTE.random()
