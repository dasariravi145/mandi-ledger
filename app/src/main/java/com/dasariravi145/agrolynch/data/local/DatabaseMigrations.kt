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

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `users` ADD COLUMN `isPremium` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `premiumExpiry` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `cloudBackupEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `multiDeviceSyncEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `voiceEntryEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `ocrEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `ocrCloudStorageEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `users` ADD COLUMN `pdfCloudStorageEnabled` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Arrivals table enhancements
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `boxCount` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `tareWeight` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `spoilageQuantity` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `netQuantity` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `laborCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `transportCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `packingCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `otherDeductions` REAL NOT NULL DEFAULT 0.0")

            // Sales table enhancements
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `laborCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `packingCharges` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `company_profile` (
                    `id` INTEGER NOT NULL, 
                    `companyName` TEXT NOT NULL, 
                    `proprietorName` TEXT NOT NULL, 
                    `mobile1` TEXT NOT NULL, 
                    `mobile2` TEXT NOT NULL, 
                    `address` TEXT NOT NULL, 
                    `village` TEXT NOT NULL, 
                    `district` TEXT NOT NULL, 
                    `state` TEXT NOT NULL, 
                    `gstNumber` TEXT NOT NULL, 
                    `licenseNumber` TEXT NOT NULL, 
                    `logoPath` TEXT, 
                    `godImagePath` TEXT, 
                    `signaturePath` TEXT, 
                    `stampPath` TEXT, 
                    `billPrefix` TEXT NOT NULL, 
                    `startingBillNumber` INTEGER NOT NULL, 
                    `nextBillNumber` INTEGER NOT NULL, 
                    `nextInvoiceNumber` INTEGER NOT NULL DEFAULT 1, 
                    `nextReceiptNumber` INTEGER NOT NULL DEFAULT 1, 
                    `billLanguage` TEXT NOT NULL, 
                    `lastUpdated` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("INSERT OR IGNORE INTO `company_profile` (id, companyName, proprietorName, mobile1, mobile2, address, village, district, state, gstNumber, licenseNumber, billPrefix, startingBillNumber, nextBillNumber, nextInvoiceNumber, nextReceiptNumber, billLanguage, lastUpdated) VALUES (1, '', '', '', '', '', '', '', '', '', '', 'BILL', 1, 1, 1, 1, 'English + Telugu', 0)")
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Sales Table updates
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `totalCommission` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `totalNetAmount` REAL NOT NULL DEFAULT 0.0")

            // 2. Sale Items Table updates
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `productCategory` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `grade` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `commissionAmount` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `laborCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `transportCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `otherCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `netAmount` REAL NOT NULL DEFAULT 0.0")

            // 3. New Performance Indexes
            // Arrivals
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_farmerId` ON `arrivals` (`farmerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_productId` ON `arrivals` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_date` ON `arrivals` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_isDeleted` ON `arrivals` (`isDeleted`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_remainingQuantity` ON `arrivals` (`remainingQuantity`)")
            
            // Farmers
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmers_name` ON `farmers` (`name`)")
            
            // Buyers
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_buyers_name` ON `buyers` (`name`)")
            
            // Payments
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_partyId` ON `payments` (`partyId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_partyType` ON `payments` (`partyType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_date` ON `payments` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_isDeleted` ON `payments` (`isDeleted`)")
            
            // Sale Items
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_arrivalId` ON `sale_items` (`arrivalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_farmerId` ON `sale_items` (`farmerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_date` ON `sale_items` (`date`)")
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `farmerName` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `commissionPercent` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `billNumber` TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ton mode columns
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `totalKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `spoilagePerTon` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `totalSpoilageKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `otherCharges` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `netPayable` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `boxWeightMode` TEXT NOT NULL DEFAULT 'AVERAGE'")

            // New table for individual boxes
            db.execSQL("CREATE TABLE IF NOT EXISTS `box_weight_items` (`id` TEXT NOT NULL, `arrivalId` TEXT NOT NULL, `boxNumber` INTEGER NOT NULL, `grossWeightKg` REAL NOT NULL, `tareWeightKg` REAL NOT NULL, `spoilageKg` REAL NOT NULL, `netWeightKg` REAL NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_box_weight_items_arrivalId` ON `box_weight_items` (`arrivalId`)")
        }
    }

    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `spoilagePercentage` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Using the correct name from ArrivalEntity
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `weightAfterEmptyBoxesKg` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Remaining Boxes mode columns
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `numberOfBoxes` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `kgPerBox` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `totalEmptyBoxWeightKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `spoilageKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `grossWeightKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `finalNetWeightKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `arrivals` ADD COLUMN `ratePerKg` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `farmerName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `productName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `productGrade` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'KG'")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `numberOfBoxes` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `kgPerBox` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `totalEmptyBoxWeightKg` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `spoilagePercentage` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `rate` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            timber.log.Timber.d("MIGRATION_37_38: Rebuilding arrivals table to fix schema mismatch")
            
            // 1. Create new table with exact schema from ArrivalEntity
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `arrivals_new` (
                    `id` TEXT NOT NULL, 
                    `farmerId` TEXT NOT NULL, 
                    `farmerName` TEXT NOT NULL, 
                    `productId` TEXT NOT NULL, 
                    `productName` TEXT NOT NULL, 
                    `productCategory` TEXT NOT NULL DEFAULT 'General', 
                    `grade` TEXT NOT NULL DEFAULT '', 
                    `quantity` REAL NOT NULL DEFAULT 0.0, 
                    `unit` TEXT NOT NULL DEFAULT 'KG', 
                    `boxCount` INTEGER NOT NULL DEFAULT 0, 
                    `tareWeight` REAL NOT NULL DEFAULT 0.0, 
                    `spoilageQuantity` REAL NOT NULL DEFAULT 0.0, 
                    `netQuantity` REAL NOT NULL DEFAULT 0.0, 
                    `remainingQuantity` REAL NOT NULL DEFAULT 0.0, 
                    `purchaseRate` REAL NOT NULL DEFAULT 0.0, 
                    `grossAmount` REAL NOT NULL DEFAULT 0.0, 
                    `commissionPercent` REAL NOT NULL DEFAULT 0.0, 
                    `commissionAmount` REAL NOT NULL DEFAULT 0.0, 
                    `laborCharges` REAL NOT NULL DEFAULT 0.0, 
                    `transportCharges` REAL NOT NULL DEFAULT 0.0, 
                    `packingCharges` REAL NOT NULL DEFAULT 0.0, 
                    `otherDeductions` REAL NOT NULL DEFAULT 0.0, 
                    `netAmount` REAL NOT NULL DEFAULT 0.0, 
                    `billNumber` TEXT NOT NULL DEFAULT '', 
                    `farmerPendingAmount` REAL NOT NULL DEFAULT 0.0, 
                    `totalKg` REAL NOT NULL DEFAULT 0.0, 
                    `spoilagePerTon` REAL NOT NULL DEFAULT 0.0, 
                    `totalSpoilageKg` REAL NOT NULL DEFAULT 0.0, 
                    `otherCharges` REAL NOT NULL DEFAULT 0.0, 
                    `netPayable` REAL NOT NULL DEFAULT 0.0, 
                    `boxWeightMode` TEXT NOT NULL DEFAULT 'AVERAGE', 
                    `numberOfBoxes` INTEGER NOT NULL DEFAULT 0, 
                    `kgPerBox` REAL NOT NULL DEFAULT 0.0, 
                    `totalEmptyBoxWeightKg` REAL NOT NULL DEFAULT 0.0, 
                    `spoilagePercentage` REAL NOT NULL DEFAULT 0.0, 
                    `spoilageKg` REAL NOT NULL DEFAULT 0.0, 
                    `grossWeightKg` REAL NOT NULL DEFAULT 0.0, 
                    `weightAfterEmptyBoxesKg` REAL NOT NULL DEFAULT 0.0, 
                    `finalNetWeightKg` REAL NOT NULL DEFAULT 0.0, 
                    `ratePerKg` REAL NOT NULL DEFAULT 0.0, 
                    `date` INTEGER NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    `isSynced` INTEGER NOT NULL DEFAULT 0, 
                    `isDeleted` INTEGER NOT NULL DEFAULT 0, 
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            // 2. Identify available columns in old table
            val cursor = db.query("PRAGMA table_info(arrivals)")
            val existingColumns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                existingColumns.add(cursor.getString(1))
            }
            cursor.close()
            
            timber.log.Timber.d("MIGRATION_37_38: Existing columns in arrivals: $existingColumns")

            // 3. Construct dynamic SELECT to handle missing columns
            val targetColumns = listOf(
                "id", "farmerId", "farmerName", "productId", "productName", "productCategory", "grade",
                "quantity", "unit", "boxCount", "tareWeight", "spoilageQuantity", "netQuantity",
                "remainingQuantity", "purchaseRate", "grossAmount", "commissionPercent", "commissionAmount",
                "laborCharges", "transportCharges", "packingCharges", "otherDeductions", "netAmount",
                "billNumber", "farmerPendingAmount", "totalKg", "spoilagePerTon", "totalSpoilageKg",
                "otherCharges", "netPayable", "boxWeightMode", "numberOfBoxes", "kgPerBox",
                "totalEmptyBoxWeightKg", "spoilagePercentage", "spoilageKg", "grossWeightKg",
                "weightAfterEmptyBoxesKg", "finalNetWeightKg", "ratePerKg", "date", "createdAt",
                "isSynced", "isDeleted"
            )

            val selectClause = targetColumns.joinToString(", ") { col ->
                if (col in existingColumns) "`$col`"
                else {
                    when (col) {
                        "productCategory" -> "'General'"
                        "boxWeightMode" -> "'AVERAGE'"
                        "unit" -> "'KG'"
                        "id", "farmerId", "farmerName", "productId", "productName", "grade", "billNumber" -> "''"
                        "numberOfBoxes", "boxCount", "isSynced", "isDeleted" -> "0"
                        "date", "createdAt" -> "strftime('%s','now')*1000"
                        else -> "0.0"
                    } + " AS `$col`"
                }
            }

            db.execSQL("INSERT INTO `arrivals_new` ($selectClause) SELECT $selectClause FROM `arrivals` ")

            // 4. Swap tables
            db.execSQL("DROP TABLE `arrivals`")
            db.execSQL("ALTER TABLE `arrivals_new` RENAME TO `arrivals`")

            // 5. Recreate Indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_farmerId` ON `arrivals` (`farmerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_productId` ON `arrivals` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_date` ON `arrivals` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_isDeleted` ON `arrivals` (`isDeleted`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_remainingQuantity` ON `arrivals` (`remainingQuantity`)")
            
            timber.log.Timber.d("MIGRATION_37_38: Migration completed successfully")
        }
    }

    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            timber.log.Timber.d("MIGRATION_38_39: Updating arrivals table for Ton-based Boxes mode")
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `arrivals_new` (
                    `id` TEXT NOT NULL, `farmerId` TEXT NOT NULL, `farmerName` TEXT NOT NULL, `productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `productCategory` TEXT NOT NULL DEFAULT 'General', `grade` TEXT NOT NULL DEFAULT '', `quantity` REAL NOT NULL DEFAULT 0.0, `unit` TEXT NOT NULL DEFAULT 'KG', `boxCount` INTEGER NOT NULL DEFAULT 0, `tareWeight` REAL NOT NULL DEFAULT 0.0, `spoilageQuantity` REAL NOT NULL DEFAULT 0.0, `netQuantity` REAL NOT NULL DEFAULT 0.0, `remainingQuantity` REAL NOT NULL DEFAULT 0.0, `purchaseRate` REAL NOT NULL DEFAULT 0.0, `grossAmount` REAL NOT NULL DEFAULT 0.0, `commissionPercent` REAL NOT NULL DEFAULT 0.0, `commissionAmount` REAL NOT NULL DEFAULT 0.0, `laborCharges` REAL NOT NULL DEFAULT 0.0, `transportCharges` REAL NOT NULL DEFAULT 0.0, `packingCharges` REAL NOT NULL DEFAULT 0.0, `otherDeductions` REAL NOT NULL DEFAULT 0.0, `netAmount` REAL NOT NULL DEFAULT 0.0, `billNumber` TEXT NOT NULL DEFAULT '', `farmerPendingAmount` REAL NOT NULL DEFAULT 0.0, `totalKg` REAL NOT NULL DEFAULT 0.0, `spoilagePerTon` REAL NOT NULL DEFAULT 0.0, `totalSpoilageKg` REAL NOT NULL DEFAULT 0.0, `otherCharges` REAL NOT NULL DEFAULT 0.0, `netPayable` REAL NOT NULL DEFAULT 0.0, `boxWeightMode` TEXT NOT NULL DEFAULT 'AVERAGE', `numberOfBoxes` INTEGER NOT NULL DEFAULT 0, `totalWeightTon` REAL NOT NULL DEFAULT 0.0, `emptyBoxWeightPerBox` REAL NOT NULL DEFAULT 0.0, `totalEmptyBoxWeightKg` REAL NOT NULL DEFAULT 0.0, `spoilagePercentage` REAL NOT NULL DEFAULT 0.0, `spoilageKg` REAL NOT NULL DEFAULT 0.0, `grossWeightKg` REAL NOT NULL DEFAULT 0.0, `weightAfterEmptyBoxesKg` REAL NOT NULL DEFAULT 0.0, `finalNetWeightKg` REAL NOT NULL DEFAULT 0.0, `ratePerKg` REAL NOT NULL DEFAULT 0.0, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL DEFAULT 0, `isDeleted` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`)
                )
            """.trimIndent())

            val targetColumns = listOf(
                "id", "farmerId", "farmerName", "productId", "productName", "productCategory", "grade",
                "quantity", "unit", "boxCount", "tareWeight", "spoilageQuantity", "netQuantity",
                "remainingQuantity", "purchaseRate", "grossAmount", "commissionPercent", "commissionAmount",
                "laborCharges", "transportCharges", "packingCharges", "otherDeductions", "netAmount",
                "billNumber", "farmerPendingAmount", "totalKg", "spoilagePerTon", "totalSpoilageKg",
                "otherCharges", "netPayable", "boxWeightMode", "numberOfBoxes", "totalWeightTon", 
                "emptyBoxWeightPerBox", "totalEmptyBoxWeightKg", "spoilagePercentage", "spoilageKg", 
                "grossWeightKg", "weightAfterEmptyBoxesKg", "finalNetWeightKg", "ratePerKg", "date", 
                "createdAt", "isSynced", "isDeleted"
            )

            val selectClause = targetColumns.joinToString(", ") { col ->
                when (col) {
                    "totalWeightTon" -> "COALESCE(`grossWeightKg`, 0.0) / 1000.0 AS `totalWeightTon`"
                    "emptyBoxWeightPerBox" -> "CASE WHEN `numberOfBoxes` > 0 THEN `totalEmptyBoxWeightKg` / `numberOfBoxes` ELSE 0.0 END AS `emptyBoxWeightPerBox`"
                    else -> "`$col`"
                }
            }

            db.execSQL("INSERT INTO `arrivals_new` SELECT $selectClause FROM `arrivals` ")
            db.execSQL("DROP TABLE `arrivals`")
            db.execSQL("ALTER TABLE `arrivals_new` RENAME TO `arrivals`")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_farmerId` ON `arrivals` (`farmerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_productId` ON `arrivals` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_date` ON `arrivals` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_isDeleted` ON `arrivals` (`isDeleted`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_arrivals_remainingQuantity` ON `arrivals` (`remainingQuantity`)")
        }
    }

    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            timber.log.Timber.d("MIGRATION_39_40: Updating ocr_scans table for Ton-based Boxes mode")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `totalWeightTon` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `ocr_scans` ADD COLUMN `emptyBoxWeightPerBox` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_40_41 = object : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            timber.log.Timber.d("MIGRATION_40_41: Rebuilding ocr_scans table to fix schema mismatch")
            
            // 1. Create new table with exact schema required by Room
            db.execSQL("""
                CREATE TABLE `ocr_scans_new` (
                    `scanId` TEXT NOT NULL, 
                    `billNumber` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `billDate` INTEGER NOT NULL, 
                    `ocrText` TEXT NOT NULL, 
                    `imageUrl` TEXT, 
                    `transactionType` TEXT NOT NULL, 
                    `farmerName` TEXT NOT NULL, 
                    `productName` TEXT NOT NULL, 
                    `productGrade` TEXT NOT NULL, 
                    `unit` TEXT NOT NULL, 
                    `numberOfBoxes` INTEGER NOT NULL, 
                    `totalWeightTon` REAL NOT NULL, 
                    `emptyBoxWeightPerBox` REAL NOT NULL, 
                    `totalEmptyBoxWeightKg` REAL NOT NULL, 
                    `spoilagePercentage` REAL NOT NULL, 
                    `rate` REAL NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`scanId`)
                )
            """.trimIndent())

            // 2. Identify available columns
            val cursor = db.query("PRAGMA table_info(ocr_scans)")
            val existingColumns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                existingColumns.add(cursor.getString(1))
            }
            cursor.close()

            // 3. Copy data with COALESCE for missing fields
            val columns = listOf(
                "scanId", "billNumber", "amount", "billDate", "ocrText", "imageUrl", 
                "transactionType", "farmerName", "productName", "productGrade", 
                "unit", "numberOfBoxes", "totalWeightTon", "emptyBoxWeightPerBox", 
                "totalEmptyBoxWeightKg", "spoilagePercentage", "rate", "createdAt"
            )

            val selectClause = columns.joinToString(", ") { col ->
                if (col in existingColumns) "`$col`"
                else {
                    when (col) {
                        "unit" -> "'KG'"
                        "numberOfBoxes" -> "0"
                        "imageUrl" -> "NULL"
                        "billDate", "createdAt" -> "strftime('%s','now')*1000"
                        "billNumber", "ocrText", "transactionType", "farmerName", "productName", "productGrade" -> "''"
                        else -> "0.0"
                    } + " AS `$col`"
                }
            }

            db.execSQL("INSERT INTO `ocr_scans_new` SELECT $selectClause FROM `ocr_scans` ")

            // 4. Swap tables
            db.execSQL("DROP TABLE `ocr_scans`")
            db.execSQL("ALTER TABLE `ocr_scans_new` RENAME TO `ocr_scans`")

            // 5. Recreate Indices
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_billDate` ON `ocr_scans` (`billDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_billNumber` ON `ocr_scans` (`billNumber`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ocr_scans_createdAt` ON `ocr_scans` (`createdAt`)")
            
            timber.log.Timber.d("MIGRATION_40_41: Migration completed successfully")
        }
    }

    val MIGRATION_41_42 = object : Migration(41, 42) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `tagline` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `upiId` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `upiQrPath` TEXT")
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `fruitImagePath` TEXT")
        }
    }

    val MIGRATION_42_43 = object : Migration(42, 43) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Add billNumber to sales and payments
            db.execSQL("ALTER TABLE `sales` ADD COLUMN `billNumber` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `payments` ADD COLUMN `billNumber` TEXT NOT NULL DEFAULT ''")

            // 2. Create bill_number_series table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `bill_number_series` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `seriesType` TEXT NOT NULL, 
                    `prefix` TEXT NOT NULL, 
                    `currentNumber` INTEGER NOT NULL, 
                    `startingNumber` INTEGER NOT NULL, 
                    `resetYearly` INTEGER NOT NULL, 
                    `financialYearEnabled` INTEGER NOT NULL, 
                    `lastGeneratedDate` INTEGER NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    `updatedAt` INTEGER NOT NULL
                )
            """.trimIndent())

            // 3. Create entry_deductions table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `entry_deductions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `entryId` TEXT NOT NULL, 
                    `entryType` TEXT NOT NULL, 
                    `billId` TEXT NOT NULL, 
                    `deductionType` TEXT NOT NULL, 
                    `customName` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `notes` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
            
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_deductions_entryId` ON `entry_deductions` (`entryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_deductions_billId` ON `entry_deductions` (`billId`)")
        }
    }

    val MIGRATION_43_44 = object : Migration(43, 44) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sale_items` ADD COLUMN `inputQuantity` REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_44_45 = object : Migration(44, 45) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `defaultTemplate` TEXT NOT NULL DEFAULT 'GK_FRUITS_CLASSIC'")
        }
    }

    val MIGRATION_45_46 = object : Migration(45, 46) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Fix BackupEntity mismatch
            db.execSQL("ALTER TABLE `backup_history` ADD COLUMN `phoneNumber` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `backup_history` ADD COLUMN `userName` TEXT NOT NULL DEFAULT ''")
            
            // Fix UserEntity mismatch (suspected missing fields in previous versions)
            val userInfo = db.query("PRAGMA table_info(users)")
            val userColumns = mutableListOf<String>()
            while (userInfo.moveToNext()) { userColumns.add(userInfo.getString(1)) }
            userInfo.close()

            if ("location" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `location` TEXT NOT NULL DEFAULT ''")
            if ("pinHash" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `pinHash` TEXT NOT NULL DEFAULT ''")
            if ("premiumPlan" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `premiumPlan` TEXT NOT NULL DEFAULT ''")
            if ("premiumStartDate" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `premiumStartDate` INTEGER NOT NULL DEFAULT 0")
            if ("premiumExpiryDate" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `premiumExpiryDate` INTEGER NOT NULL DEFAULT 0")
            if ("purchaseToken" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `purchaseToken` TEXT NOT NULL DEFAULT ''")
            if ("productId" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `productId` TEXT NOT NULL DEFAULT ''")
            if ("lastUpdatedAt" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `lastUpdatedAt` INTEGER NOT NULL DEFAULT 0")
            if ("createdAt" !in userColumns) db.execSQL("ALTER TABLE `users` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_46_47 = object : Migration(46, 47) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `marketName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `city` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `customTemplatePath` TEXT")
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `template_positions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `templateType` TEXT NOT NULL, 
                    `fieldKey` TEXT NOT NULL, 
                    `x` REAL NOT NULL, 
                    `y` REAL NOT NULL, 
                    `width` REAL NOT NULL DEFAULT 0.0, 
                    `height` REAL NOT NULL DEFAULT 0.0, 
                    `fontSize` REAL NOT NULL DEFAULT 12.0, 
                    `fontColor` INTEGER NOT NULL DEFAULT -16777216, 
                    `alignment` TEXT NOT NULL DEFAULT 'LEFT', 
                    `isVisible` INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
        }
    }

    val MIGRATION_47_48 = object : Migration(47, 48) {
        override fun migrate(db: SupportSQLiteDatabase) {
            timber.log.Timber.d("MIGRATION_47_48: Fixing various schema mismatches")
            
            // 1. Fix products table
            val productInfo = db.query("PRAGMA table_info(products)")
            val productColumns = mutableListOf<String>()
            while (productInfo.moveToNext()) { productColumns.add(productInfo.getString(1)) }
            productInfo.close()

            if ("category" !in productColumns) db.execSQL("ALTER TABLE `products` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''")
            if ("imageUrl" !in productColumns) db.execSQL("ALTER TABLE `products` ADD COLUMN `imageUrl` TEXT NOT NULL DEFAULT ''")
            if ("availableGrades" !in productColumns) db.execSQL("ALTER TABLE `products` ADD COLUMN `availableGrades` TEXT NOT NULL DEFAULT '[]'")

            // 2. Fix company_profile table
            val profileInfo = db.query("PRAGMA table_info(company_profile)")
            val profileColumns = mutableListOf<String>()
            while (profileInfo.moveToNext()) { profileColumns.add(profileInfo.getString(1)) }
            profileInfo.close()

            if ("pincode" !in profileColumns) db.execSQL("ALTER TABLE `company_profile` ADD COLUMN `pincode` TEXT NOT NULL DEFAULT ''")

            // 3. Ensure isSynced and isDeleted exist in all major tables
            val tablesToFix = listOf("farmers", "buyers", "products", "transactions", "arrivals", "sales", "payments", "market_rates")
            tablesToFix.forEach { table ->
                val info = db.query("PRAGMA table_info($table)")
                val cols = mutableListOf<String>()
                while (info.moveToNext()) { cols.add(info.getString(1)) }
                info.close()

                if ("isSynced" !in cols) db.execSQL("ALTER TABLE `$table` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
                if ("isDeleted" !in cols) db.execSQL("ALTER TABLE `$table` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    val MIGRATION_48_49 = object : Migration(48, 49) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `xPercent` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `yPercent` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `widthPercent` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `heightPercent` REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `backgroundColor` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `borderColor` INTEGER NOT NULL DEFAULT -7829368")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `borderEnabled` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `bold` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `template_positions` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            
            // Initialize percentages for existing data (assuming A4 default if x/y were points)
            db.execSQL("UPDATE `template_positions` SET `xPercent` = `x` / 595.0, `yPercent` = `y` / 842.0 WHERE `x` > 0 OR `y` > 0")
        }
    }

    val MIGRATION_49_50 = object : Migration(49, 50) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `backup_history` ADD COLUMN `storagePath` TEXT NOT NULL DEFAULT ''")
        }
    }
}
