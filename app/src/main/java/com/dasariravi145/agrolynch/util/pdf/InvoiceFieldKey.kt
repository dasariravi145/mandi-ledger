package com.dasariravi145.agrolynch.util.pdf

object InvoiceFieldKey {
    const val COMPANY_NAME = "companyName"
    const val COMPANY_ADDRESS = "companyAddress"
    const val COMPANY_MOBILE = "companyMobile"
    const val GST_NUMBER = "gstNumber"
    const val TAGLINE = "tagline"
    
    const val BILL_NO = "billNo"
    const val DATE = "date"
    const val PARTY_NAME = "partyName"
    const val PARTY_MOBILE = "partyMobile"
    
    const val SUB_TOTAL = "subTotal"
    const val COMMISSION = "commission"
    const val TRANSPORT = "transport"
    const val LABOUR = "labour"
    const val ADVANCE = "advance"
    const val OTHERS = "others"
    const val GRAND_TOTAL = "grandTotal"
    
    const val QR_CODE = "qrCode"
    const val SIGNATURE = "signature"
    const val STAMP = "stamp"
    const val COMPANY_LOGO = "companyLogo"
    
    // For Custom Template positioning
    const val TABLE_START = "tableStart"
    const val TOTALS_BOX = "totalsBox"
    const val COMPANY_PHONE = "companyPhone" // Map to mobile1
    
    fun getAllKeys() = listOf(
        COMPANY_NAME, COMPANY_ADDRESS, COMPANY_MOBILE, COMPANY_PHONE, GST_NUMBER, TAGLINE,
        BILL_NO, DATE, PARTY_NAME, PARTY_MOBILE,
        SUB_TOTAL, COMMISSION, TRANSPORT, LABOUR, ADVANCE, OTHERS, GRAND_TOTAL,
        QR_CODE, SIGNATURE, STAMP, COMPANY_LOGO, TABLE_START, TOTALS_BOX
    )
}
