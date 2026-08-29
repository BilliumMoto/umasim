package io.github.mee1080.umasim.race.calc2

import io.github.mee1080.umasim.race.data2.SkillCondition
import io.github.mee1080.umasim.race.data2.SkillData
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RaceCalculatorTest {

    @Test
    fun testGoalSpIsRecorded() {
        val setting = RaceSetting()
        val calculator = RaceCalculator(SystemSetting())
        val (result, state) = calculator.simulate(setting)

        // Ensure that goalSp is recorded and matches the final state stamina (simulation.sp)
        assertTrue(result.goalSp >= -10000.0, "goalSp should be a valid double value")
        assertTrue(result.goalSp == state.simulation.sp, "goalSp should equal the remaining stamina at the goal")
    }

    @Test
    fun successionSkillCountCountsOnlyInheritedSkills() {
        assertFalse(checkSuccessionSkillCount(listOf("inherit", "inherit", "normal")))
        assertTrue(checkSuccessionSkillCount(listOf("inherit", "inherit", "inherit")))
    }

    @Test
    fun unsupportedSkillConditionsFailDuringCheckerConstruction() {
        assertFailsWith<IllegalArgumentException> {
            checkCondition(
                testSkill("normal"),
                listOf(listOf(SkillCondition("future_condition", "==", 1))),
                RaceSetting(),
                mutableMapOf(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            checkCondition(
                testSkill("normal"),
                listOf(listOf(SkillCondition("always", "!=", 1))),
                RaceSetting(),
                mutableMapOf(),
            )
        }
    }

    private fun checkSuccessionSkillCount(rarities: List<String>): Boolean {
        val skills = rarities.mapIndexed { index, rarity -> testSkill(rarity, index.toString()) }
        val setting = RaceSetting(umaStatus = UmaStatus(hasSkills = skills))
        val state = RaceState(
            RaceSettingWithPassive(setting, PassiveBonus()),
            RaceSimulationState(),
            SystemSetting(),
            null,
        )
        val check = checkCondition(
            testSkill("normal"),
            listOf(listOf(SkillCondition("succession_skill_count", ">=", 3))),
            setting,
            mutableMapOf(),
        )
        return check(state)
    }

    private fun testSkill(rarity: String, suffix: String = "") = SkillData(
        id = "test$suffix",
        name = "test",
        rarity = rarity,
        group = 0,
        type = "speed",
    )
}
