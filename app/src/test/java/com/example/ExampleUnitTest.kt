package com.example

import com.example.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDinosaurSpeciesDatabaseRoster() {
        val dinos = PrehistoricDatabase.dinosaurs
        assertTrue("At least 12 dinosaur species must be configured", dinos.size >= 12)

        val raptor = dinos.find { it.id == "raptor" }
        assertNotNull(raptor)
        assertTrue("Velociraptor should be unlocked by default", raptor!!.unlockedByDefault)
        assertEquals(DietType.CARNIVORE, raptor.diet)

        val rex = dinos.find { it.id == "trex" }
        assertNotNull(rex)
        assertEquals(Rarity.LEGENDARY, rex!!.rarity)
    }

    @Test
    fun testGrowthStageMultipliers() {
        val egg = GrowthStage.EGG
        val adult = GrowthStage.ADULT
        val elder = GrowthStage.ELDER

        assertTrue("Adult should have larger scale multiplier than egg", adult.scaleMultiplier > egg.scaleMultiplier)
        assertTrue("Adult should have higher damage multiplier than egg", adult.damageMultiplier > egg.damageMultiplier)
        assertTrue("Elder should have highest scale multiplier", elder.scaleMultiplier >= adult.scaleMultiplier)
    }

    @Test
    fun testInitialQuestsAndAchievements() {
        val quests = PrehistoricDatabase.initialQuests
        assertTrue("Initial quests should contain 8 story objectives", quests.size >= 8)

        val achievements = PrehistoricDatabase.initialAchievements
        assertTrue("Initial achievements should be configured", achievements.isNotEmpty())
    }

    @Test
    fun testItemCategoriesAndConsumables() {
        val berries = PrehistoricDatabase.items.find { it.id == "berries" }
        assertNotNull(berries)
        assertTrue("Berries must restore hunger", berries!!.hungerAmount > 0f)

        val water = PrehistoricDatabase.items.find { it.id == "clean_water" }
        assertNotNull(water)
        assertTrue("Water must restore hydration", water!!.hydrationAmount > 0f)
    }
}
