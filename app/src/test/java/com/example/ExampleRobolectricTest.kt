package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.repository.DnsRepository
import com.example.ui.DnsAppScreen
import com.example.ui.DnsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Simple DNS Configurator", appName)
  }

  @Test
  fun `launch main activity`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `render dns app screen successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    
    val repository = DnsRepository(
      serverDao = db.dnsServerDao(),
      logDao = db.queryLogDao()
    )
    val viewModel = DnsViewModel(repository)

    composeTestRule.setContent {
      DnsAppScreen(viewModel = viewModel)
    }

    // Verify critical components are built correctly and available in the tree
    composeTestRule.onNodeWithTag("open_dns_settings_button").assertExists()

    db.close()
  }
}
