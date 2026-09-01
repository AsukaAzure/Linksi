package com.linksi.app.utils

import android.content.Context
import android.net.Uri
import com.linksi.app.R
import com.linksi.app.data.repository.LinkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundImportManager @Inject constructor(
    private val repository: LinkRepository,
    @ApplicationContext private val context: Context
) {
    data class ImportProgress(
        val isImporting: Boolean = false,
        val phase: String = "",
        val progress: Int = 0,
        val total: Int = 0,
        val message: String? = null,
        val result: ImportResult? = null,
        val duplicateCount: Int = 0,
        val isMinimized: Boolean = false
    )

    private val _progress = MutableStateFlow(ImportProgress())
    val progress = _progress.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var importJob: Job? = null

    fun startImport(uri: Uri) {
        if (_progress.value.isImporting) return
        
        importJob = scope.launch {
            try {
                _progress.update { it.copy(
                    isImporting = true, 
                    phase = context.getString(R.string.reading_file_phase),
                    isMinimized = false,
                    message = null,
                    result = null
                ) }

                val fileName = getFileName(context, uri).lowercase()
                val mimeType = context.contentResolver.getType(uri)
                val result = withContext(Dispatchers.IO) {
                    when {
                        fileName.endsWith(".json") || mimeType == "application/json" -> importFromLinksJson(context, uri)
                        fileName.endsWith(".csv") || mimeType == "text/csv" -> importFromCsv(context, uri)
                        else -> importFromBrowserHtml(context, uri)
                    }
                }

                // Insert folders — maintaining hierarchy
                val folderIdMap = mutableMapOf<Long, Long>()
                
                val remainingFolders = result.folders.toMutableList()
                var passes = 0
                val maxPasses = 100 // Prevent infinite loops for circular dependencies
                
                while (remainingFolders.isNotEmpty() && passes < maxPasses) {
                    val iterator = remainingFolders.iterator()
                    var insertedThisPass = 0
                    while (iterator.hasNext()) {
                        val folder = iterator.next()
                        // If root folder or parent already mapped
                        if (folder.parentId == null || folderIdMap.containsKey(folder.parentId)) {
                            val targetParentId = folder.parentId?.let { folderIdMap[it] }
                            val existing = repository.getFolderByNameAndParent(folder.name, targetParentId)
                            val newId = if (existing != null) {
                                existing.id
                            } else {
                                repository.insertFolder(
                                    folder.copy(
                                        id = 0,
                                        parentId = targetParentId
                                    )
                                )
                            }
                            folderIdMap[folder.id] = newId
                            iterator.remove()
                            insertedThisPass++
                        }
                    }
                    if (insertedThisPass == 0) {
                        // We are stuck (likely a broken hierarchy or circular dependency)
                        // Break the cycle by inserting the next folder as a root folder
                        val folder = remainingFolders.removeAt(0)
                        val existing = repository.getFolderByNameAndParent(folder.name, null)
                        val newId = existing?.id ?: repository.insertFolder(folder.copy(id = 0, parentId = null))
                        folderIdMap[folder.id] = newId
                    }
                    passes++
                }

                var importedCount = 0
                var duplicateCount = 0
                val insertedLinks = mutableListOf<Pair<Long, String>>()

                _progress.update { it.copy(
                    phase = context.getString(R.string.importing_links_phase),
                    total = result.links.size,
                    progress = 0
                )}

                result.links.forEachIndexed { index, link ->
                    val normalizedUrl = normalizeUrl(link.url)
                    val existing = repository.getLinkByUrl(normalizedUrl)
                    
                    if (existing != null) {
                        if (existing.inBin) {
                            // Restore from bin
                            repository.restoreFromBin(existing.id)
                            // Update folder if needed
                            val targetFolderId = link.folderId?.let { folderIdMap[it] }
                            if (existing.folderId != targetFolderId) {
                                repository.moveToFolder(existing.id, targetFolderId)
                            }
                            insertedLinks.add(existing.id to normalizedUrl)
                            importedCount++
                        } else {
                            duplicateCount++
                        }
                    } else {
                        val newId = repository.insertLink(
                            link.copy(
                                url = normalizedUrl,
                                folderId = link.folderId?.let { folderIdMap[it] }
                            )
                        )
                        insertedLinks.add(newId to normalizedUrl)
                        importedCount++
                    }
                    _progress.update { it.copy(progress = index + 1) }
                }

                // Fetch metadata in parallel
                _progress.update { it.copy(
                    phase = context.getString(R.string.fetching_metadata_phase),
                    total = insertedLinks.size,
                    progress = 0
                )}

                if (insertedLinks.isNotEmpty()) {
                    MetadataFetcher.fetchAll(
                        urls = insertedLinks.map { it.second },
                        context = context,
                        concurrency = 8,
                        onItemComplete = { url, meta ->
                            scope.launch(Dispatchers.IO) {
                                val entry = insertedLinks.find { it.second == url } ?: return@launch
                                val id = entry.first
                                
                                // Save to cache
                                repository.saveMetadataToCache(url, meta)
                                
                                val existing = repository.getLinkById(id) ?: return@launch
                                
                                // Only update if existing data is missing or just domain-placeholder
                                val currentTitle = existing.title
                                val currentDomain = existing.domain
                                val shouldUpdateTitle = currentTitle.isBlank() || 
                                                       currentTitle == currentDomain || 
                                                       currentTitle == url ||
                                                       currentTitle == extractDomain(url)

                                val updatedTitle = if (shouldUpdateTitle && meta.title.isNotBlank()) {
                                    meta.title.take(200)
                                } else {
                                    currentTitle
                                }

                                repository.updateLink(existing.copy(
                                    title = updatedTitle,
                                    description = if (existing.description.isBlank()) meta.description.take(500) else existing.description,
                                    faviconUrl = if (existing.faviconUrl.isBlank()) meta.faviconUrl else existing.faviconUrl,
                                    previewImageUrl = if (existing.previewImageUrl.isBlank()) meta.previewImageUrl else existing.previewImageUrl,
                                    domain = if (existing.domain.isBlank()) meta.domain else existing.domain
                                ))
                                
                                _progress.update { it.copy(progress = it.progress + 1) }
                            }
                        }
                    )
                }

                val finalMessage = if (duplicateCount > 0) {
                    context.getString(R.string.import_message_with_duplicates, importedCount, duplicateCount)
                } else {
                    context.getString(R.string.import_message_success, importedCount)
                }

                _progress.update { it.copy(
                    isImporting = false,
                    phase = "",
                    progress = 0,
                    total = 0,
                    result = result.copy(count = importedCount),
                    duplicateCount = duplicateCount,
                    message = finalMessage,
                    isMinimized = false
                )}

            } catch (e: Exception) {
                _progress.update { it.copy(
                    isImporting = false,
                    phase = "",
                    message = context.getString(R.string.import_failed_msg, e.message),
                    isMinimized = false
                )}
            }
        }
    }

    fun minimize() {
        _progress.update { it.copy(isMinimized = true) }
    }

    fun dismiss() {
        if (!_progress.value.isImporting) {
            _progress.update { it.copy(message = null, result = null, isMinimized = false) }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: ""
    }
}
