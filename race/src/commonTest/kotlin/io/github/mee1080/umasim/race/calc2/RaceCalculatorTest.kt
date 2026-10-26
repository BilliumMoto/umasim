package io.github.mee1080.umasim.race.calc2

import io.github.mee1080.umasim.race.data2.SkillCondition
import io.github.mee1080.umasim.race.data2.SkillData
import io.github.mee1080.umasim.race.data2.SkillEffect
import io.github.mee1080.umasim.race.data2.Invoke
import kotlin.test.Test
import kotlin.test.assertEquals
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
                CalculatedConditions(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            checkCondition(
                testSkill("normal"),
                listOf(listOf(SkillCondition("always", "!=", 1))),
                RaceSetting(),
                CalculatedConditions(),
            )
        }
    }

    @Test
    fun passiveSkillSelectsFirstMatchingInvoke() {
        val skill = allInSkill()

        val (_, ordinaryState) = RaceCalculator(SystemSetting()).simulate(
            RaceSetting(
                umaStatus = UmaStatus(hasSkills = listOf(skill)),
                track = Track(location = 10006, course = 10604),
            )
        )
        assertEquals(60, ordinaryState.setting.passiveBonus.speed)
        assertEquals(60, ordinaryState.setting.passiveBonus.power)
        assertEquals(2, ordinaryState.setting.passiveBonus.skills.single().invoke.index)
        assertFalse(ordinaryState.simulation.ignorePopularity)

        val (_, specialState) = RaceCalculator(SystemSetting()).simulate(
            RaceSetting(
                umaStatus = UmaStatus(hasSkills = listOf(skill)),
                track = Track(location = 10009, course = 10906),
            )
        )
        assertEquals(60, specialState.setting.passiveBonus.speed)
        assertEquals(60, specialState.setting.passiveBonus.power)
        assertEquals(1, specialState.setting.passiveBonus.skills.single().invoke.index)
        assertTrue(specialState.simulation.ignorePopularity)
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
            CalculatedConditions(),
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

    private fun allInSkill() = SkillData(
        id = "104901311",
        name = "All-in!!",
        rarity = "evo",
        group = 20265,
        type = "multi",
        activateLot = 0,
        invokes = listOf(
            Invoke(
                skillId = "104901311",
                index = 1,
                preConditions = listOf(
                    listOf(
                        SkillCondition("track_id", "==", 10009),
                        SkillCondition("course_distance", "==", 2200),
                    ),
                    listOf(SkillCondition("track_id", "==", 10201)),
                ),
                conditions = listOf(listOf(SkillCondition("distance_type", "==", 3))),
                effects = passiveEffects + SkillEffect("ignorePopularity", 10000),
                cd = 0.0,
            ),
            Invoke(
                skillId = "104901311",
                index = 2,
                conditions = listOf(listOf(SkillCondition("distance_type", "==", 3))),
                effects = passiveEffects,
                cd = 0.0,
            ),
        ),
    )

    private val passiveEffects
        get() = listOf(
            SkillEffect("passiveSpeed", 600000),
            SkillEffect("passivePower", 600000),
        )

    @Test
    fun testFullSpurtFrameRecording() {
        val setting = RaceSetting(
            umaStatus = UmaStatus(speed = 2200)
        )
        val calculator = RaceCalculator(SystemSetting())
        val (_, state) = calculator.simulate(setting)

        val fullSpurtFrames = state.simulation.frames.filter { it.fullSpurt }
        if (fullSpurtFrames.isNotEmpty()) {
            fullSpurtFrames.forEach { frame ->
                assertTrue(frame.fullSpurtTargetSpeed > 0.0, "fullSpurtTargetSpeed should be recorded when fullSpurt is active")
                val normalTargetSpeed = frame.targetSpeed - frame.fullSpurtTargetSpeed
                val normalCurrentSpeed = frame.speed - frame.fullSpurtCurrentSpeed
                val speedDiff = normalTargetSpeed - normalCurrentSpeed
                assertTrue(!speedDiff.isNaN() && !speedDiff.isInfinite(), "speedDiff without fullSpurt should be a finite double")
            }
        }
    }
}
