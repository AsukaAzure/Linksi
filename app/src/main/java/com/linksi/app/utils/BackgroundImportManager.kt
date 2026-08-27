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

                val fileName = uri.path?.lowercase() ?: ""
                val result = withContext(Dispatchers.IO) {
                    when {
                        fileName.endsWith(".json") -> importFromLinksJson(context, uri)
                        fileName.endsWith(".csv") -> importFromCsv(context, uri)
                        else -> importFromBrowserHtml(context, uri)
                    }
                }

                // Insert folders — maintaining hierarchy
                val folderIdMap = mutableMapOf<Long, Long>()
                // Group by parentId to process level by level or just sort by parentId
                // Simple approach: Iterate multiple times until all folders are inserted
                val remainingFolders = result.folders.toMutableList()
                while (remainingFolders.isNotEmpty()) {
                    val iterator = remainingFolders.iterator()
                    var insertedThisPass = 0
                    while (iterator.hasNext()) {
                        val folder = iterator.next()
                        // If root folder or its parent is already inserted
                        if (folder.parentId == null || folderIdMap.containsKey(folder.parentId)) {
                            val existing = repository.getFolderByName(folder.name)
                            val newId = if (existing != null) {
                                existing.id
                            } else {
                                repository.insertFolder(
                                    folder.copy(
                                        id = 0,
                                        parentId = folder.parentId?.let { folderIdMap[it] }
                                    )
                                )
                            }
                            folderIdMap[folder.id] = newId
                            iterator.remove()
                            insertedThisPass++
                        }
                    }
                    if (insertedThisPass == 0) {
                        // Avoid infinite loop if there's a circular dependency or missing parent
                        // Just insert the rest as root folders
                        remainingFolders.forEach { folder ->
                            val existing = repository.getFolderByName(folder.name)
                            val newId = existing?.id ?: repository.insertFolder(folder.copy(id = 0, parentId = null))
                            folderIdMap[folder.id] = newId
                        }
                        remainingFolders.clear()
                    }
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
                    if (repository.isUrlAlreadySaved(link.url)) {
                        duplicateCount++
                    } else {
                        val newId = repository.insertLink(
                            link.copy(folderId = link.folderId?.let { folderIdMap[it] })
                        )
                        insertedLinks.add(newId to link.url)
                        importedCount++
                    }
                    _progress.update { it.copy(progress = index + 1) }
                }

                // Fetch metadata
                _progress.update { it.copy(
                    phase = context.getString(R.string.fetching_metadata_phase),
                    total = insertedLinks.size,
                    progress = 0
                )}

                insertedLinks.forEachIndexed { index, (id, url) ->
                    try {
                        val existing = repository.getLinkById(id)
                        val needsFetch = existing?.title.isNullOrBlank() ||
                                existing?.title == existing?.domain ||
                                existing?.previewImageUrl.isNullOrBlank()

                        if (needsFetch) {
                            val meta = MetadataFetcher.fetch(url)
                            existing?.let {
                                repository.updateLink(it.copy(
                                    title = meta.title.ifBlank { it.title.ifBlank { extractDomain(url) } },
                                    description = meta.description.ifBlank { it.description },
                                    faviconUrl = meta.faviconUrl.ifBlank { it.faviconUrl },
                                    previewImageUrl = meta.previewImageUrl,
                                    domain = meta.domain.ifBlank { it.domain }
                                ))
                            }
                        }
                    } catch (e: Exception) { }
                    _progress.update { it.copy(progress = index + 1) }
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
}
