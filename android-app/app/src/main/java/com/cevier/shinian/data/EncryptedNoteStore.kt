package com.cevier.shinian.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cevier.shinian.model.Note
import com.cevier.shinian.model.NoteType
import com.cevier.shinian.model.randomNoteAccent
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptedNoteStore(private val context: Context) {
    private val file = context.filesDir.resolve("shinian-notes.bin")
    private val mediaDirectory = context.filesDir.resolve("media").apply { mkdirs() }
    private val previewDirectory = context.filesDir.resolve("media-previews").apply { mkdirs() }

    @Synchronized
    fun load(): List<Note> {
        if (!file.exists()) return emptyList()

        return runCatching {
            val root = JSONObject(decrypt(file.readBytes()).decodeToString())
            val notes = decodeNotes(root)
            if (root.optInt("version", 1) < STORE_VERSION) {
                val migrated = migrateNotes(notes, root.optInt("version", 1))
                save(migrated)
                migrated
            } else {
                notes
            }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun save(notes: List<Note>) {
        file.writeBytes(encrypt(encodeNotes(notes).encodeToByteArray()))
    }

    fun importMedia(uri: Uri, extension: String): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取所选文件")
        return saveMedia(bytes, extension).also { path ->
            if (extension.isImageExtension()) saveImagePreview(path, bytes)
        }
    }

    fun importMedia(source: File, extension: String): String {
        val bytes = source.readBytes()
        val path = saveMedia(bytes, extension)
        if (extension.isImageExtension()) saveImagePreview(path, bytes)
        source.delete()
        return path
    }

    fun loadMedia(path: String): ByteArray = decrypt(resolveMedia(path).readBytes())

    fun loadImagePreview(path: String): ByteArray {
        val preview = resolvePreview(path)
        if (preview.exists()) return decrypt(preview.readBytes())

        val original = loadMedia(path)
        return saveImagePreview(path, original) ?: original
    }

    fun createPlaybackFile(path: String, extension: String): File {
        val directory = context.cacheDir.resolve("playback").apply { mkdirs() }
        return File.createTempFile("shinian-", ".$extension", directory).apply {
            writeBytes(loadMedia(path))
        }
    }

    fun deleteMedia(path: String?) {
        path?.let {
            resolveMedia(it).delete()
            resolvePreview(it).delete()
        }
    }

    fun export(context: Context, uri: Uri, format: String, notes: List<Note>, backupPassphrase: String? = null) {
        when (format) {
            "加密备份" -> exportPortableBackup(
                context = context,
                uri = uri,
                notes = notes,
                passphrase = requireNotNull(backupPassphrase) { "请输入恢复口令" },
            )

            "通用 JSON" -> context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(encodeNotes(notes).encodeToByteArray())
            }

            "PDF 文档" -> writePdf(context, uri, notes)
        }
    }

    private fun exportPortableBackup(context: Context, uri: Uri, notes: List<Note>, passphrase: String) {
        require(passphrase.length >= 8) { "恢复口令至少需要 8 个字符" }
        val salt = ByteArray(BACKUP_SALT_SIZE).also(SecureRandom()::nextBytes)
        val iv = ByteArray(BACKUP_IV_SIZE).also(SecureRandom()::nextBytes)
        val header = portableBackupHeader(salt, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, derivePortableKey(passphrase, salt, BACKUP_KDF_ITERATIONS), GCMParameterSpec(128, iv))
            updateAAD(header)
        }
        val output = context.contentResolver.openOutputStream(uri) ?: error("无法创建备份文件")
        BufferedOutputStream(output).use { target ->
            target.write(header)
            target.flush()
            CipherOutputStream(target, cipher).use { encrypted ->
                ZipOutputStream(BufferedOutputStream(encrypted)).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_NOTES_ENTRY))
                    zip.write(encodeNotes(notes).encodeToByteArray())
                    zip.closeEntry()

                    notes.mapNotNull(Note::mediaPath).distinct().forEach { path ->
                        val media = resolveMedia(path)
                        require(media.exists()) { "记录媒体文件缺失" }
                        zip.putNextEntry(ZipEntry("$BACKUP_MEDIA_PREFIX$path"))
                        zip.write(loadMedia(path))
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    @Synchronized
    fun importPortableBackup(context: Context, uri: Uri, passphrase: String): List<Note> {
        require(passphrase.length >= 8) { "恢复口令至少需要 8 个字符" }
        val staging = context.cacheDir.resolve("backup-import-${UUID.randomUUID()}").apply { mkdirs() }
        val decryptedArchive = staging.resolve("payload.zip")
        val extractedMedia = staging.resolve("media").apply { mkdirs() }
        val createdMedia = mutableListOf<String>()

        try {
            val source = context.contentResolver.openInputStream(uri) ?: error("无法读取备份文件")
            BufferedInputStream(source).use { input ->
                val header = ByteArray(BACKUP_HEADER_SIZE)
                DataInputStream(input).readFully(header)
                val parsed = parsePortableBackupHeader(header)
                val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        derivePortableKey(passphrase, parsed.salt, parsed.iterations),
                        GCMParameterSpec(128, parsed.iv),
                    )
                    updateAAD(header)
                }
                CipherInputStream(input, cipher).use { decrypted ->
                    FileOutputStream(decryptedArchive).buffered().use { output ->
                        decrypted.copyToWithLimit(output, MAX_BACKUP_ARCHIVE_BYTES)
                    }
                }
            }

            var notesJson: String? = null
            val mediaFiles = mutableMapOf<String, File>()
            var extractedBytes = 0L
            var entryCount = 0
            ZipInputStream(BufferedInputStream(FileInputStream(decryptedArchive))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    require(entryCount <= MAX_BACKUP_ENTRIES) { "备份文件包含过多项目" }
                    require(!entry.isDirectory) { "备份文件结构无效" }
                    when {
                        entry.name == BACKUP_NOTES_ENTRY -> {
                            require(notesJson == null) { "备份文件包含重复的记录清单" }
                            val bytes = zip.readBytesWithLimit(MAX_NOTES_JSON_BYTES)
                            extractedBytes += bytes.size
                            notesJson = bytes.decodeToString()
                        }

                        entry.name.startsWith(BACKUP_MEDIA_PREFIX) -> {
                            val name = entry.name.removePrefix(BACKUP_MEDIA_PREFIX)
                            require(name.isNotBlank() && name == File(name).name) { "备份媒体路径无效" }
                            require(name !in mediaFiles) { "备份文件包含重复媒体" }
                            val target = extractedMedia.resolve(name)
                            FileOutputStream(target).buffered().use { output ->
                                extractedBytes += zip.copyToWithLimit(output, MAX_MEDIA_ENTRY_BYTES)
                            }
                            mediaFiles[name] = target
                        }

                        else -> error("备份文件包含未知内容")
                    }
                    require(extractedBytes <= MAX_BACKUP_EXTRACTED_BYTES) { "备份文件过大" }
                    zip.closeEntry()
                }
            }

            val notesRoot = JSONObject(requireNotNull(notesJson) { "备份中缺少记录清单" })
            val decodedNotes = migrateNotes(
                decodeNotes(notesRoot),
                notesRoot.optInt("version", 1),
            )
            val referencedMedia = decodedNotes.mapNotNull(Note::mediaPath).toSet()
            require(referencedMedia == mediaFiles.keys) { "备份中的媒体文件不完整" }

            val importedNotes = decodedNotes.map { note ->
                val originalPath = note.mediaPath ?: return@map note
                val mediaBytes = requireNotNull(mediaFiles[originalPath]).readBytes()
                val extension = originalPath.removeSuffix(".enc").substringAfterLast('.', "bin")
                val newPath = saveMedia(mediaBytes, extension)
                createdMedia += newPath
                if (note.type == NoteType.IMAGE) saveImagePreview(newPath, mediaBytes)
                note.copy(mediaPath = newPath)
            }
            save(importedNotes)
            return importedNotes
        } catch (error: Exception) {
            createdMedia.forEach(::deleteMedia)
            val message = error.message.orEmpty()
            if (message.startsWith("当前备份") || message.startsWith("旧版备份")) throw error
            throw IllegalArgumentException("恢复口令不正确，或备份文件已经损坏", error)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun saveMedia(bytes: ByteArray, extension: String): String {
        val safeExtension = extension.lowercase().filter(Char::isLetterOrDigit).ifBlank { "bin" }
        val name = "${UUID.randomUUID()}.$safeExtension.enc"
        mediaDirectory.resolve(name).writeBytes(encrypt(bytes))
        return name
    }

    private fun resolveMedia(path: String): File {
        require(path == File(path).name) { "非法媒体路径" }
        return mediaDirectory.resolve(path)
    }

    private fun resolvePreview(path: String): File {
        require(path == File(path).name) { "非法媒体路径" }
        return previewDirectory.resolve("$path.preview.enc")
    }

    private fun saveImagePreview(path: String, bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > PREVIEW_MAX_DIMENSION || bounds.outHeight / sampleSize > PREVIEW_MAX_DIMENSION) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) },
        ) ?: return null
        return try {
            ByteArrayOutputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PREVIEW_JPEG_QUALITY, output)) return null
                output.toByteArray().also { previewBytes ->
                    resolvePreview(path).writeBytes(encrypt(previewBytes))
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun String.isImageExtension(): Boolean =
        lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "heif")

    private fun writePdf(context: Context, uri: Uri, notes: List<Note>) {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF182237.toInt()
            textSize = 24f
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2F6FED.toInt()
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF4F5B6D.toInt()
            textSize = 13f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 60f
        canvas.drawText("拾念 · 本地记录", 42f, y, titlePaint)
        y += 42f

        notes.forEach { note ->
            val required = 84f + (note.body.length / 24) * 18f
            if (y + required > 790f) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                y = 54f
            }
            canvas.drawText("${note.day} ${note.time}  ·  ${note.type.label}", 42f, y, metaPaint)
            y += 26f
            canvas.drawText(note.title, 42f, y, titlePaint.apply { textSize = 17f })
            y += 25f
            note.body.chunked(30).forEach { line ->
                canvas.drawText(line, 42f, y, bodyPaint)
                y += 19f
            }
            y += 24f
        }

        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use(document::writeTo)
        document.close()
    }

    private fun encodeNotes(notes: List<Note>): String {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject().apply {
                put("id", note.id)
                put("day", note.day)
                put("time", note.time)
                put("type", note.type.name)
                put("duration", note.duration)
                put("title", note.title)
                put("body", note.body)
                put("accent", note.accent)
                put("mediaPath", note.mediaPath)
                put("recordedDate", note.recordedDate)
            })
        }
        return JSONObject().put("version", STORE_VERSION).put("notes", array).toString(2)
    }

    private fun decodeNotes(root: JSONObject): List<Note> {
        val array = root.getJSONArray("notes")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Note(
                        id = item.getLong("id"),
                        day = item.getString("day"),
                        time = item.getString("time"),
                        type = NoteType.valueOf(item.getString("type")),
                        duration = item.optNullableString("duration"),
                        title = item.getString("title"),
                        body = item.getString("body"),
                        accent = item.getLong("accent"),
                        mediaPath = item.optNullableString("mediaPath"),
                        recordedDate = item.optNullableString("recordedDate"),
                    ),
                )
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" }

    private fun isLegacyDemoNote(note: Note): Boolean =
        note.id in 1L..5L && note.title in LEGACY_DEMO_TITLES

    private fun migrateNotes(notes: List<Note>, fromVersion: Int): List<Note> {
        var migrated = notes
        if (fromVersion < 2) migrated = migrated.filterNot(::isLegacyDemoNote)
        if (fromVersion < 3) migrated = migrated.map { it.copy(accent = randomNoteAccent()) }
        return migrated
    }

    private fun portableBackupHeader(salt: ByteArray, iv: ByteArray): ByteArray =
        ByteArrayOutputStream(BACKUP_HEADER_SIZE).use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.write(BACKUP_MAGIC)
                output.writeInt(BACKUP_FORMAT_VERSION)
                output.writeInt(BACKUP_KDF_ITERATIONS)
                output.write(salt)
                output.write(iv)
            }
            buffer.toByteArray()
        }

    private fun parsePortableBackupHeader(header: ByteArray): PortableBackupHeader {
        require(header.size == BACKUP_HEADER_SIZE) { "备份文件头无效" }
        return DataInputStream(ByteArrayInputStream(header)).use { input ->
            val magic = ByteArray(BACKUP_MAGIC.size).also(input::readFully)
            if (!magic.contentEquals(BACKUP_MAGIC)) {
                if (magic.take(2).toByteArray().contentEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte()))) {
                    error("旧版备份无法跨设备恢复，请在旧手机上使用新版 App 重新导出")
                }
                error("当前备份文件格式不受支持")
            }
            val version = input.readInt()
            require(version == BACKUP_FORMAT_VERSION) { "当前备份版本不受支持" }
            val iterations = input.readInt()
            require(iterations in MIN_BACKUP_KDF_ITERATIONS..MAX_BACKUP_KDF_ITERATIONS) { "备份加密参数无效" }
            val salt = ByteArray(BACKUP_SALT_SIZE).also(input::readFully)
            val iv = ByteArray(BACKUP_IV_SIZE).also(input::readFully)
            PortableBackupHeader(iterations, salt, iv)
        }
    }

    private fun derivePortableKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKey {
        val specification = PBEKeySpec(passphrase.toCharArray(), salt, iterations, BACKUP_KEY_LENGTH_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
            SecretKeySpec(bytes, KeyProperties.KEY_ALGORITHM_AES)
        } finally {
            specification.clearPassword()
        }
    }

    private fun InputStream.copyToWithLimit(output: java.io.OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            copied += count
            require(copied <= limit) { "备份文件过大" }
            output.write(buffer, 0, count)
        }
        return copied
    }

    private fun InputStream.readBytesWithLimit(limit: Long): ByteArray =
        ByteArrayOutputStream().use { output ->
            copyToWithLimit(output, limit)
            output.toByteArray()
        }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = cipher.doFinal(plain)
        return ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(cipher.iv.size)
                output.write(cipher.iv)
                output.writeInt(payload.size)
                output.write(payload)
            }
            buffer.toByteArray()
        }
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        DataInputStream(ByteArrayInputStream(encrypted)).use { input ->
            val iv = ByteArray(input.readInt()).also(input::readFully)
            val payload = ByteArray(input.readInt()).also(input::readFully)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            return cipher.doFinal(payload)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private val NoteType.label: String
        get() = when (this) {
            NoteType.TEXT -> "文字"
            NoteType.IMAGE -> "图片"
            NoteType.AUDIO -> "音频"
        }

    private data class PortableBackupHeader(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
    )

    private companion object {
        const val STORE_VERSION = 3
        const val KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "shinian_local_notes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREVIEW_MAX_DIMENSION = 1280
        const val PREVIEW_JPEG_QUALITY = 86
        val BACKUP_MAGIC = "SHINIAN2".encodeToByteArray()
        const val BACKUP_FORMAT_VERSION = 1
        const val BACKUP_KDF_ITERATIONS = 210_000
        const val MIN_BACKUP_KDF_ITERATIONS = 100_000
        const val MAX_BACKUP_KDF_ITERATIONS = 1_000_000
        const val BACKUP_KEY_LENGTH_BITS = 256
        const val BACKUP_SALT_SIZE = 16
        const val BACKUP_IV_SIZE = 12
        const val BACKUP_HEADER_SIZE = 8 + 4 + 4 + BACKUP_SALT_SIZE + BACKUP_IV_SIZE
        const val BACKUP_NOTES_ENTRY = "notes.json"
        const val BACKUP_MEDIA_PREFIX = "media/"
        const val MAX_BACKUP_ENTRIES = 10_000
        const val MAX_NOTES_JSON_BYTES = 16L * 1024L * 1024L
        const val MAX_MEDIA_ENTRY_BYTES = 512L * 1024L * 1024L
        const val MAX_BACKUP_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MAX_BACKUP_EXTRACTED_BYTES = 2L * 1024L * 1024L * 1024L
        val LEGACY_DEMO_TITLES = setOf(
            "散步时想到的产品判断",
            "雨停后的窗边",
            "关于专注",
            "周末阅读随记",
            "会议后的补充",
        )
    }
}
