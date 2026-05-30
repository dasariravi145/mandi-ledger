package com.dasariravi145.agrolynch.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`transactionId` TEXT NOT NULL, `userId` TEXT NOT NULL, `userName` TEXT NOT NULL, `planName` TEXT NOT NULL, `amount` TEXT NOT NULL, `status` TEXT NOT NULL, `purchaseDate` INTEGER NOT NULL, `expiryDate` INTEGER NOT NULL, `accountReceived` TEXT NOT NULL, `orderId` TEXT NOT NULL, PRIMARY KEY(`transactionId`))")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `commissionPercent` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `commissionAmount` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `netAmount` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `grade` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `backup_history` (`id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `fileName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `size` INTEGER NOT NULL, `type` TEXT NOT NULL, `reportType` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Keep previous version logic if needed, but we'll fix it in 19_20
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Recreate Farmers
            db.execSQL("DROP TABLE IF EXISTS `farmers_new`")
            db.execSQL("CREATE TABLE `farmers_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `mobileNumber` TEXT NOT NULL, `village` TEXT NOT NULL, `notes` TEXT NOT NULL, `totalArrivals` REAL NOT NULL, `totalPayments` REAL NOT NULL, `pendingAmount` REAL NOT NULL, `advanceAmount` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            
            val farmerInfo = db.query("PRAGMA table_info(farmers)")
            val farmerColumns = mutableListOf<String>()
            while (farmerInfo.moveToNext()) { farmerColumns.add(farmerInfo.getString(1)) }
            farmerInfo.close()

            if (farmerColumns.isNotEmpty()) {
                val hasTotalArrivals = "totalArrivals" in farmerColumns
                val hasTotalPayments = "totalPayments" in farmerColumns
                val hasPendingAmount = "pendingAmount" in farmerColumns
                val hasAdvanceAmount = "advanceAmount" in farmerColumns
                
                val arrivalsCol = if (hasTotalArrivals) "totalArrivals" else "0.0"
                val paymentsCol = if (hasTotalPayments) "totalPayments" else "0.0"
                val pendingCol = if (hasPendingAmount) "pendingAmount" else "0.0"
                val advanceCol = if (hasAdvanceAmount) "advanceAmount" else "0.0"

                db.execSQL("INSERT INTO `farmers_new` (id, name, mobileNumber, village, notes, totalArrivals, totalPayments, pendingAmount, advanceAmount, lastUpdated, isSynced, isDeleted) " +
                           "SELECT id, name, mobileNumber, village, notes, $arrivalsCol, $paymentsCol, $pendingCol, $advanceCol, lastUpdated, isSynced, isDeleted FROM `farmers`")
                db.execSQL("DROP TABLE `farmers`")
            }
            db.execSQL("ALTER TABLE `farmers_new` RENAME TO `farmers`")

            // 2. Recreate Buyers
            db.execSQL("DROP TABLE IF EXISTS `buyers_new`")
            db.execSQL("CREATE TABLE `buyers_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `mobileNumber` TEXT NOT NULL, `address` TEXT NOT NULL, `gstNumber` TEXT NOT NULL, `notes` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `totalPurchase` REAL NOT NULL, `totalPaid` REAL NOT NULL, `pendingAmount` REAL NOT NULL, PRIMARY KEY(`id`))")
            
            val buyerInfo = db.query("PRAGMA table_info(buyers)")
            val buyerColumns = mutableListOf<String>()
            while (buyerInfo.moveToNext()) { buyerColumns.add(buyerInfo.getString(1)) }
            buyerInfo.close()

            if (buyerColumns.isNotEmpty()) {
                val hasTP = "totalPurchase" in buyerColumns
                val hasTPaid = "totalPaid" in buyerColumns
                val hasPA = "pendingAmount" in buyerColumns
                
                val tpCol = if (hasTP) "totalPurchase" else "0.0"
                val tpaidCol = if (hasTPaid) "totalPaid" else "0.0"
                val paCol = if (hasPA) "pendingAmount" else "0.0"

                db.execSQL("INSERT INTO `buyers_new` (id, name, mobileNumber, address, gstNumber, notes, lastUpdated, isSynced, isDeleted, totalPurchase, totalPaid, pendingAmount) " +
                           "SELECT id, name, mobileNumber, address, gstNumber, notes, lastUpdated, isSynced, isDeleted, $tpCol, $tpaidCol, $paCol FROM `buyers`")
                db.execSQL("DROP TABLE `buyers`")
            }
            db.execSQL("ALTER TABLE `buyers_new` RENAME TO `buyers`")

            // 3. Recreate Arrivals
            db.execSQL("DROP TABLE IF EXISTS `arrivals_new`")
            db.execSQL("CREATE TABLE `arrivals_new` (`id` TEXT NOT NULL, `farmerId` TEXT NOT NULL, `farmerName` TEXT NOT NULL, `productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `grade` TEXT NOT NULL, `quantity` REAL NOT NULL, `remainingQuantity` REAL NOT NULL, `unit` TEXT NOT NULL, `purchaseRate` REAL NOT NULL, `grossAmount` REAL NOT NULL, `commissionPercent` REAL NOT NULL, `commissionAmount` REAL NOT NULL, `netAmount` REAL NOT NULL, `farmerPendingAmount` REAL NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            
            val arrivalInfo = db.query("PRAGMA table_info(arrivals)")
            val arrivalColumns = mutableListOf<String>()
            while (arrivalInfo.moveToNext()) { arrivalColumns.add(arrivalInfo.getString(1)) }
            arrivalInfo.close()

            if (arrivalColumns.isNotEmpty()) {
                val hasRate = "rate" in arrivalColumns
                val rateCol = if (hasRate) "rate" else if ("purchaseRate" in arrivalColumns) "purchaseRate" else "0.0"
                val hasRemQ = "remainingQuantity" in arrivalColumns
                val remQCol = if (hasRemQ) "remainingQuantity" else "quantity"
                val hasGrade = "grade" in arrivalColumns
                val gradeCol = if (hasGrade) "grade" else "''"
                
                db.execSQL("INSERT INTO `arrivals_new` (id, farmerId, farmerName, productId, productName, grade, quantity, remainingQuantity, unit, purchaseRate, grossAmount, commissionPercent, commissionAmount, netAmount, farmerPendingAmount, date, createdAt, isSynced, isDeleted) " +
                           "SELECT id, farmerId, farmerName, productId, productName, $gradeCol, quantity, $remQCol, 'KG', $rateCol, grossAmount, commissionPercent, commissionAmount, netAmount, 0.0, date, createdAt, isSynced, isDeleted FROM `arrivals`")
                db.execSQL("DROP TABLE `arrivals`")
            }
            db.execSQL("ALTER TABLE `arrivals_new` RENAME TO `arrivals`")

            // 4. Recreate Sales
            db.execSQL("DROP TABLE IF EXISTS `sales_new`")
            db.execSQL("CREATE TABLE `sales_new` (`id` TEXT NOT NULL, `buyerId` TEXT NOT NULL, `buyerName` TEXT NOT NULL, `productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `grade` TEXT NOT NULL, `totalQuantity` REAL NOT NULL, `totalPurchaseAmount` REAL NOT NULL, `totalAmount` REAL NOT NULL, `totalMargin` REAL NOT NULL, `transportCharges` REAL NOT NULL, `otherCharges` REAL NOT NULL, `paidAmount` REAL NOT NULL, `pendingAmount` REAL NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            
            val saleInfo = db.query("PRAGMA table_info(sales)")
            val saleColumns = mutableListOf<String>()
            while (saleInfo.moveToNext()) { saleColumns.add(saleInfo.getString(1)) }
            saleInfo.close()

            if (saleColumns.isNotEmpty()) {
                val qCol = if ("quantity" in saleColumns) "quantity" else if ("totalQuantity" in saleColumns) "totalQuantity" else "0.0"
                val rCol = if ("rate" in saleColumns) "rate" else "0.0"
                val hasPA = "paidAmount" in saleColumns
                val paCol = if (hasPA) "paidAmount" else "0.0"
                val hasPenA = "pendingAmount" in saleColumns
                val penACol = if (hasPenA) "pendingAmount" else "0.0"
                
                db.execSQL("INSERT INTO `sales_new` (id, buyerId, buyerName, productId, productName, grade, totalQuantity, totalPurchaseAmount, totalAmount, totalMargin, transportCharges, otherCharges, paidAmount, pendingAmount, date, createdAt, isSynced, isDeleted) " +
                           "SELECT id, buyerId, buyerName, productId, productName, grade, $qCol, ($qCol * $rCol), totalAmount, 0.0, transportCharges, 0.0, $paCol, $penACol, date, createdAt, isSynced, isDeleted FROM `sales`")
                db.execSQL("DROP TABLE `sales`")
            }
            db.execSQL("ALTER TABLE `sales_new` RENAME TO `sales`")

            // 5. Recreate Payments
            db.execSQL("DROP TABLE IF EXISTS `payments_new`")
            db.execSQL("CREATE TABLE `payments_new` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `partyName` TEXT NOT NULL, `partyType` TEXT NOT NULL, `amount` REAL NOT NULL, `paymentMode` TEXT NOT NULL, `referenceNumber` TEXT NOT NULL, `remainingBalance` REAL NOT NULL, `advanceAmount` REAL NOT NULL, `notes` TEXT NOT NULL, `date` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            
            val paymentInfo = db.query("PRAGMA table_info(payments)")
            val paymentColumns = mutableListOf<String>()
            while (paymentInfo.moveToNext()) { paymentColumns.add(paymentInfo.getString(1)) }
            paymentInfo.close()

            if (paymentColumns.isNotEmpty()) {
                val hasRB = "remainingBalance" in paymentColumns
                val hasAA = "advanceAmount" in paymentColumns
                val rbCol = if (hasRB) "remainingBalance" else "0.0"
                val aaCol = if (hasAA) "advanceAmount" else "0.0"

                db.execSQL("INSERT INTO `payments_new` (id, partyId, partyName, partyType, amount, paymentMode, referenceNumber, remainingBalance, advanceAmount, notes, date, lastUpdated, isSynced, isDeleted) " +
                           "SELECT id, partyId, partyName, partyType, amount, paymentMode, referenceNumber, $rbCol, $aaCol, notes, date, lastUpdated, isSynced, isDeleted FROM `payments`")
                db.execSQL("DROP TABLE `payments`")
            }
            db.execSQL("ALTER TABLE `payments_new` RENAME TO `payments`")

            // 6. Ensure Sale Items table exists
            db.execSQL("CREATE TABLE IF NOT EXISTS `sale_items` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `arrivalId` TEXT NOT NULL, `farmerId` TEXT NOT NULL, `farmerName` TEXT NOT NULL, `productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `quantitySold` REAL NOT NULL, `unit` TEXT NOT NULL, `purchaseRate` REAL NOT NULL, `saleRate` REAL NOT NULL, `purchaseAmount` REAL NOT NULL, `saleAmount` REAL NOT NULL, `marginAmount` REAL NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `dashboard_summary` (`id` INTEGER NOT NULL, `todaySales` REAL NOT NULL, `todayCommission` REAL NOT NULL, `totalCommission` REAL NOT NULL, `buyerPending` REAL NOT NULL, `farmerPending` REAL NOT NULL, `netBalance` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("INSERT OR REPLACE INTO `dashboard_summary` (id, todaySales, todayCommission, totalCommission, buyerPending, farmerPending, netBalance, updatedAt) VALUES (1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)")
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add productCategory to arrivals
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `productCategory` TEXT NOT NULL DEFAULT 'General'")
            
            // Add indexes to products
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_isDeleted` ON `products` (`isDeleted`)")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create missing indexes for Farmers
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmers_mobileNumber` ON `farmers` (`mobileNumber`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmers_village` ON `farmers` (`village`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmers_isDeleted` ON `farmers` (`isDeleted`)")

            // Create missing indexes for Buyers
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_buyers_mobileNumber` ON `buyers` (`mobileNumber`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_buyers_isDeleted` ON `buyers` (`isDeleted`)")

            // Create missing indexes for Sales
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_buyerId` ON `sales` (`buyerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_date` ON `sales` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_isDeleted` ON `sales` (`isDeleted`)")
            
            // Add any other missing system-critical indexes
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create ocr_scans table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `ocr_scans` (
                    `scanId` TEXT NOT NULL, 
                    `billNumber` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `billDate` INTEGER NOT NULL, 
                    `ocrText` TEXT NOT NULL, 
                    `imageUrl` TEXT, 
                    `transactionType` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`scanId`)
                )
            """.trimIndent())
            
            // Fix any other potential missing tables or columns
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_billDate` ON `ocr_scans` (`billDate`)")
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Robustly ensure ocr_scans has the correct schema
            // We'll rename the old table, create the new one, and copy data if possible
            db.execSQL("DROP TABLE IF EXISTS `ocr_scans_old`")
            
            // Check if ocr_scans exists
            val tableExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='ocr_scans'").moveToFirst()
            
            if (tableExists) {
                db.execSQL("ALTER TABLE `ocr_scans` RENAME TO `ocr_scans_old`")
            }

            db.execSQL("""
                CREATE TABLE `ocr_scans` (
                    `scanId` TEXT NOT NULL, 
                    `billNumber` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `billDate` INTEGER NOT NULL, 
                    `ocrText` TEXT NOT NULL, 
                    `imageUrl` TEXT, 
                    `transactionType` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`scanId`)
                )
            """.trimIndent())

            if (tableExists) {
                // Try to copy data from old table, filling missing columns with defaults
                val cursor = db.query("PRAGMA table_info(ocr_scans_old)")
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(1))
                }
                cursor.close()

                val selectColumns = mutableListOf<String>()
                
                // Primary fields
                selectColumns.add("scanId")
                selectColumns.add(if ("billNumber" in columns) "billNumber" else "''")
                selectColumns.add(if ("amount" in columns) "amount" else "0.0")
                selectColumns.add(if ("billDate" in columns) "billDate" else "createdAt") // fallback to createdAt if billDate missing
                selectColumns.add(if ("ocrText" in columns) "ocrText" else "''")
                selectColumns.add(if ("imageUrl" in columns) "imageUrl" else "NULL")
                selectColumns.add(if ("transactionType" in columns) "transactionType" else "''")
                selectColumns.add("createdAt")

                db.execSQL("""
                    INSERT INTO `ocr_scans` (scanId, billNumber, amount, billDate, ocrText, imageUrl, transactionType, createdAt)
                    SELECT scanId, ${selectColumns[1]}, ${selectColumns[2]}, ${selectColumns[3]}, ${selectColumns[4]}, ${selectColumns[5]}, ${selectColumns[6]}, createdAt FROM `ocr_scans_old`
                """.trimIndent())
                
                db.execSQL("DROP TABLE `ocr_scans_old`")
            }

            // Create Indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_billDate` ON `ocr_scans` (`billDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_billNumber` ON `ocr_scans` (`billNumber`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_createdAt` ON `ocr_scans` (`createdAt`)")
        }
    }
}
