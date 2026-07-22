package com.dasariravi145.agrolynch.domain.repository

import com.dasariravi145.agrolynch.data.local.entity.AccountBookArchiveEntity
import com.dasariravi145.agrolynch.domain.model.LedgerSummary
import com.dasariravi145.agrolynch.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ArchiveRepository {
    fun getArchives(): Flow<List<AccountBookArchiveEntity>>
    suspend fun getArchiveById(id: String): AccountBookArchiveEntity?
    suspend fun createArchive(summary: LedgerSummary, partyType: String): Resource<String>
    suspend fun deleteLiveHistory(archiveId: String): Resource<Unit>
    suspend fun restoreArchive(archiveId: String): Resource<Unit>
    suspend fun permanentDeleteArchive(archiveId: String): Resource<Unit>
    suspend fun exportArchivePdf(context: android.content.Context, archiveId: String): Resource<File>
    suspend fun exportArchiveExcel(context: android.content.Context, archiveId: String): Resource<File>
}
