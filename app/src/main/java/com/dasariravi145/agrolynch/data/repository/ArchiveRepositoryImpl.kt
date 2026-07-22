package com.dasariravi145.agrolynch.data.repository

import androidx.room.withTransaction
import com.dasariravi145.agrolynch.data.local.AgroLynchDatabase
import com.dasariravi145.agrolynch.data.local.entity.AccountBookArchiveEntity
import com.dasariravi145.agrolynch.domain.model.BackupData
import com.dasariravi145.agrolynch.domain.model.LedgerSummary
import com.dasariravi145.agrolynch.domain.repository.ArchiveRepository
import com.dasariravi145.agrolynch.domain.repository.CompanyRepository
import com.dasariravi145.agrolynch.util.DeduplicationManager
import com.dasariravi145.agrolynch.util.ReportExportService
import com.dasariravi145.agrolynch.util.Resource
import com.dasariravi145.agrolynch.util.pdf.TemplateInvoicePdfService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepositoryImpl @Inject constructor(
    private val database: AgroLynchDatabase,
    private val gson: Gson,
    private val pdfService: TemplateInvoicePdfService,
    private val reportExportService: ReportExportService,
    private val companyRepository: CompanyRepository,
    private val deduplicationManager: DeduplicationManager
) : ArchiveRepository {

    private val archiveDao = database.accountBookArchiveDao()
    private val arrivalDao = database.arrivalDao()
    private val saleDao = database.saleDao()
    private val paymentDao = database.paymentDao()
    private val transactionDao = database.transactionDao()
    private val deductionDao = database.entryDeductionDao()
    private val farmerDao = database.farmerDao()
    private val buyerDao = database.buyerDao()

    override fun getArchives(): Flow<List<AccountBookArchiveEntity>> = archiveDao.getAllArchives()

    override suspend fun getArchiveById(id: String): AccountBookArchiveEntity? = archiveDao.getArchiveById(id)

    override suspend fun createArchive(summary: LedgerSummary, partyType: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            if (summary.balance != 0.0) {
                return@withContext Resource.Error("Account must be settled before archiving.")
            }

            val archiveId = UUID.randomUUID().toString()
            val partyId = summary.partyId
            
            val snapshot = if (partyType == "FARMER") {
                val arrivals = arrivalDao.getArrivalsByFarmer(partyId)
                val payments = paymentDao.getPaymentsByParty(partyId, "FARMER")
                val legacyTrans = transactionDao.getTransactionsByFarmer(partyId)
                val saleItems = saleDao.getItemsByFarmer(partyId)
                
                val deductions = arrivalDao.getDeductionsForFarmerArrivals(partyId)

                BackupData(
                    arrivals = arrivals,
                    payments = payments,
                    transactions = legacyTrans,
                    saleItems = saleItems,
                    deductions = deductions
                )
            } else {
                val sales = saleDao.getSalesByBuyer(partyId)
                val payments = paymentDao.getPaymentsByParty(partyId, "BUYER")
                val saleItems = saleDao.getItemsByBuyerSales(partyId)
                val deductions = saleDao.getDeductionsForBuyerSales(partyId)

                BackupData(
                    sales = sales,
                    saleItems = saleItems,
                    payments = payments,
                    deductions = deductions
                )
            }

            val partyPhone = if (partyType == "FARMER") {
                farmerDao.getFarmerById(partyId)?.mobileNumber ?: ""
            } else {
                buyerDao.getBuyerById(partyId)?.mobileNumber ?: ""
            }

            val snapshotJson = withContext(Dispatchers.Default) {
                gson.toJson(snapshot)
            }

            val archive = AccountBookArchiveEntity(
                archiveId = archiveId,
                partyType = partyType,
                originalPartyId = partyId,
                partyName = summary.partyName,
                partyPhone = partyPhone,
                totalAmount = summary.totalDebit,
                paidAmount = summary.totalCredit,
                pendingAmount = summary.balance,
                settlementDate = summary.lastTransactionDate,
                snapshotJson = snapshotJson
            )

            archiveDao.insertArchive(archive)
            Resource.Success(archiveId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create archive snapshot")
        }
    }

    override suspend fun deleteLiveHistory(archiveId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val archive = archiveDao.getArchiveById(archiveId) ?: return@withContext Resource.Error("Archive not found")
            val snapshot = gson.fromJson(archive.snapshotJson, BackupData::class.java)

            database.withTransaction {
                arrivalDao.softDeleteArrivals(snapshot.arrivals.map { it.id })
                saleDao.softDeleteSales(snapshot.sales.map { it.id })
                paymentDao.softDeletePayments(snapshot.payments.map { it.id })
                transactionDao.softDeleteTransactions(snapshot.transactions.map { it.id })
                
                val allDeductionEntryIds = snapshot.deductions.map { it.entryId }.distinct()
                allDeductionEntryIds.chunked(500).forEach { chunk ->
                    deductionDao.deleteByEntryIds(chunk)
                }
                
                archiveDao.updateArchive(archive.copy(status = "LIVE_HISTORY_DELETED"))
            }
            
            // Recalculate master balances
            if (archive.partyType == "FARMER") {
                deduplicationManager.recalculateFarmerBalances(archive.originalPartyId)
            } else {
                deduplicationManager.recalculateBuyerBalances(archive.originalPartyId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete live history")
        }
    }

    override suspend fun restoreArchive(archiveId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val archive = archiveDao.getArchiveById(archiveId) ?: return@withContext Resource.Error("Archive not found")
            val snapshot = gson.fromJson(archive.snapshotJson, BackupData::class.java)

            // Resolve canonical party ID (Identity remapping)
            val currentPartyId = archive.originalPartyId
            val partyPhone = archive.partyPhone
            
            val canonicalPartyId = if (archive.partyType == "FARMER") {
                val existing = farmerDao.getFarmerById(currentPartyId)
                if (existing != null) currentPartyId
                else {
                    val normalized = com.dasariravi145.agrolynch.util.PhoneNumberUtils.normalize(partyPhone)
                    val match = farmerDao.getFarmersList().find { 
                        com.dasariravi145.agrolynch.util.PhoneNumberUtils.normalize(it.mobileNumber) == normalized 
                    }
                    match?.id ?: currentPartyId
                }
            } else {
                val existing = buyerDao.getBuyerById(currentPartyId)
                if (existing != null) currentPartyId
                else {
                    val normalized = com.dasariravi145.agrolynch.util.PhoneNumberUtils.normalize(partyPhone)
                    val match = buyerDao.getBuyersList().find { 
                        com.dasariravi145.agrolynch.util.PhoneNumberUtils.normalize(it.mobileNumber) == normalized 
                    }
                    match?.id ?: currentPartyId
                }
            }

            database.withTransaction {
                snapshot.arrivals.forEach { arrivalDao.insertArrival(it.copy(farmerId = canonicalPartyId, isDeleted = false, isSynced = false)) }
                snapshot.sales.forEach { saleDao.insertSale(it.copy(buyerId = canonicalPartyId, isDeleted = false, isSynced = false)) }
                snapshot.payments.forEach { paymentDao.insertPayment(it.copy(partyId = canonicalPartyId, isDeleted = false, isSynced = false)) }
                snapshot.transactions.forEach { transactionDao.insertTransaction(it.copy(farmerId = canonicalPartyId, isDeleted = false, isSynced = false)) }
                snapshot.saleItems.forEach { item ->
                    val fid = if (archive.partyType == "FARMER") canonicalPartyId else item.farmerId
                    saleDao.insertSaleItem(item.copy(farmerId = fid))
                }
                
                snapshot.deductions.forEach { deductionDao.insertDeduction(it) }
                
                archiveDao.updateArchive(archive.copy(status = "RESTORED"))
            }
            
            // Recalculate
            if (archive.partyType == "FARMER") {
                deduplicationManager.recalculateFarmerBalances(canonicalPartyId)
            } else {
                deduplicationManager.recalculateBuyerBalances(canonicalPartyId)
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to restore archive")
        }
    }

    override suspend fun permanentDeleteArchive(archiveId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            archiveDao.deleteArchiveById(archiveId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete archive")
        }
    }

    override suspend fun exportArchivePdf(context: android.content.Context, archiveId: String): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val archive = archiveDao.getArchiveById(archiveId) ?: return@withContext Resource.Error("Archive not found")
            val snapshot = gson.fromJson(archive.snapshotJson, BackupData::class.java)
            val profile = companyRepository.getProfile().first() ?: com.dasariravi145.agrolynch.data.local.entity.CompanyProfileEntity()
            
            val file = pdfService.generateArchivePdf(context, profile, archive, snapshot)
            if (file != null && file.exists()) Resource.Success(file)
            else Resource.Error("Failed to generate PDF file")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Export failed")
        }
    }

    override suspend fun exportArchiveExcel(context: android.content.Context, archiveId: String): Resource<File> = withContext(Dispatchers.IO) {
        try {
            val archive = archiveDao.getArchiveById(archiveId) ?: return@withContext Resource.Error("Archive not found")
            val snapshot = gson.fromJson(archive.snapshotJson, BackupData::class.java)
            
            // Combine all transactions into one list for CSV export
            val list = mutableListOf<Any>()
            list.addAll(snapshot.arrivals)
            list.addAll(snapshot.sales)
            list.addAll(snapshot.payments)
            
            val file = reportExportService.exportToExcel(context, "Archive_${archive.partyName}", list)
            if (file != null && file.exists()) Resource.Success(file)
            else Resource.Error("Failed to generate Excel file")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Export failed")
        }
    }
}
