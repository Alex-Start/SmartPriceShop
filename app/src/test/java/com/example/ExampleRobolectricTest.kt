package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Good
import com.example.data.model.PriceRecord
import com.example.data.model.Shop
import com.example.data.repository.PriceRepository
import com.example.util.UnitPriceCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Price", appName)
  }

  @Test
  fun `unit price calculation for grams and kilograms`() {
    // 500g butter for $2.50 -> $0.50 per 100g
    val (unitPrice1, label1) = UnitPriceCalculator.calculateUnitPrice(2.50, 500.0, "g")
    assertEquals(0.50, unitPrice1, 0.001)
    assertEquals("$/100g", label1)

    // 2kg apples for $4.00 -> $0.20 per 100g
    val (unitPrice2, label2) = UnitPriceCalculator.calculateUnitPrice(4.00, 2.0, "kg")
    assertEquals(0.20, unitPrice2, 0.001)
    assertEquals("$/100g", label2)

    // 6 items for $3.00 -> $0.50 per item
    val (unitPrice3, label3) = UnitPriceCalculator.calculateUnitPrice(3.00, 6.0, "item")
    assertEquals(0.50, unitPrice3, 0.001)
    assertEquals("$/item", label3)
  }

  @Test
  fun `room database repository operations`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val repo = PriceRepository(db)

    // Save shop
    val shop = repo.getOrCreateShop("Test Mart", "123 Main St")
    assertTrue(shop.id > 0)

    // Save good
    val goodId = repo.saveGood(Good(name = "Organic Bananas", category = "Fruits & Veg"))
    assertTrue(goodId > 0)

    // Save price record
    repo.savePriceRecord(
      goodId = goodId,
      shopId = shop.id,
      regularPrice = 1.99,
      discountPrice = 1.49,
      packageAmount = 1.0,
      packageUnit = "kg"
    )

    val good = repo.getGoodById(goodId)
    assertEquals("Organic Bananas", good?.name)
  }
}
