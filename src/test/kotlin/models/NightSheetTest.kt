package models

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NightSheetTest {

  @Test
  fun fromJson_parsesCorrectly() {
    val json = """{
      "firstNight": ["Demoninfo", "Minioninfo", "Cook", "Fortune Teller"],
      "otherNight": ["Butler", "Monk", "Ravenkeeper", "Fortune Teller"]
    }"""
    
    val nightSheet = NightSheet.fromJson(Gson(), json)
    
    assertThat(nightSheet.firstNight).containsExactly(
      "Demoninfo", "Minioninfo", "Cook", "Fortune Teller"
    )
    assertThat(nightSheet.otherNight).containsExactly(
      "Butler", "Monk", "Ravenkeeper", "Fortune Teller"
    )
  }

  @Test
  fun fromJson_emptyLists() {
    val json = """{
      "firstNight": [],
      "otherNight": []
    }"""
    
    val nightSheet = NightSheet.fromJson(Gson(), json)
    
    assertThat(nightSheet.firstNight).isEmpty()
    assertThat(nightSheet.otherNight).isEmpty()
  }
}