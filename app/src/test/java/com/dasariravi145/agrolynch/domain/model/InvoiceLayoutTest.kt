package com.dasariravi145.agrolynch.domain.model

import org.junit.Test
import org.junit.Assert.*

class InvoiceLayoutTest {
    @Test
    fun testDefaultLayouts() {
        val config = InvoiceWizardConfig()
        val shopNameLayout = config.getLayout(InvoiceWizardConfig.KEY_SHOP_NAME)
        
        assertEquals(36.5f, shopNameLayout.xPercent)
        assertEquals(21.8f, shopNameLayout.yPercent)
        assertTrue(shopNameLayout.visible)
    }

    @Test
    fun testCustomLayouts() {
        val customLayout = ElementLayout(xPercent = 25f, yPercent = 30f)
        val config = InvoiceWizardConfig(
            layouts = mapOf(InvoiceWizardConfig.KEY_SHOP_NAME to customLayout)
        )
        
        val layout = config.getLayout(InvoiceWizardConfig.KEY_SHOP_NAME)
        assertEquals(25f, layout.xPercent)
        assertEquals(30f, layout.yPercent)
    }
}
