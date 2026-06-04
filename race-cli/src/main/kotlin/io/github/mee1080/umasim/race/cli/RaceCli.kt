package io.github.mee1080.umasim.race.cli

import io.github.mee1080.umasim.data.CharaLoader
import io.github.mee1080.umasim.race.calc2.DebuffType
import io.github.mee1080.umasim.race.calc2.RaceCalculator
import io.github.mee1080.umasim.race.calc2.RaceFrame
import io.github.mee1080.umasim.race.calc2.RaceSetting
import io.github.mee1080.umasim.race.calc2.RaceSimulationResult
import io.github.mee1080.umasim.race.calc2.SystemSetting
import io.github.mee1080.umasim.race.calc2.Track
import io.github.mee1080.umasim.race.calc2.UmaStatus
import io.github.mee1080.umasim.race.data.Condition
import io.github.mee1080.umasim.race.data.CourseCondition
import io.github.mee1080.umasim.race.data.FitRank
import io.github.mee1080.umasim.race.data.PositionKeepMode
import io.github.mee1080.umasim.race.data.RandomPosition
import io.github.mee1080.umasim.race.data.SkillActivateAdjustment
import io.github.mee1080.umasim.race.data.Style
import io.github.mee1080.umasim.race.data.TrackDetail
import io.github.mee1080.umasim.race.data.loadRecentEventTrackListFromString
import io.github.mee1080.umasim.race.data.trackData
import io.github.mee1080.umasim.race.data2.SkillData
import io.github.mee1080.umasim.race.data2.findSkills
import io.github.mee1080.umasim.race.data2.loadSkillDataFromString
import io.github.mee1080.umasim.race.data2.skillData2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import kotlin.system.exitProcess

@Serializable
data class RaceCliRequest(
    val count: Int = 1,
    val threads: Int = 1,
    val dataDir: String = "data",
    val skillDataPath: String? = null,
    val eventTrackPath: String? = null,
    val includeRuns: Boolean = true,
    val includeFirstRunFrames: Boolean = false,
    val maxFrames: Int? = null,
    val uma: UmaStatusInput = UmaStatusInput(),
    val track: TrackInput? = null,
    val setting: RaceSettingInput = RaceSettingInput(),
    val system: SystemSettingInput = SystemSettingInput(),
)

@Serializable
data class UmaStatusInput(
    val charaName: String? = null,
    val speed: Int? = null,
    val stamina: Int? = null,
    val power: Int? = null,
    val guts: Int? = null,
    val wisdom: Int? = null,
    val condition: String? = null,
    val style: String? = null,
    val distanceFit: String? = null,
    val surfaceFit: String? = null,
    val styleFit: String? = null,
    val popularity: Int? = null,
    val gateNumber: Int? = null,
    val skills: List<String> = emptyList(),
    val uniqueLevel: Int? = null,
)

@Serializable
data class TrackInput(
    val location: Int? = null,
    val course: Int? = null,
    val condition: String? = null,
    val gateCount: Int? = null,
)

@Serializable
data class RaceSettingInput(
    val skillActivateAdjustment: String? = null,
    val randomPosition: String? = null,
    val season: Int? = null,
    val weather: Int? = null,
    val badStart: Boolean? = null,
    val debuffCounts: Map<String, Int> = emptyMap(),
    val positionKeepMode: String? = null,
    val positionKeepRate: Int? = null,
    val virtualLeader: UmaStatusInput? = null,
)

@Serializable
data class SystemSettingInput(
    val skillLaneChangeRate: Double? = null,
    val positionCompetitionRate: Double? = null,
    val competeFightRate: Double? = null,
    val secureLeadRate: Double? = null,
)

@Serializable
data class RaceCliResponse(
    val count: Int,
    val summary: RaceSummaryOutput,
    val runs: List<RaceRunOutput> = emptyList(),
    val firstRunFrames: List<RaceFrameOutput> = emptyList(),
)

@Serializable
data class RaceSummaryOutput(
    val averageTime: Double,
    val bestTime: Double,
    val worstTime: Double,
    val averageTimeWithoutRunUp: Double,
    val averageTimeDelta: Double,
    val averageSp: Double,
    val bestSp: Double,
    val worstSp: Double,
    val maxSpurtRate: Double,
    val staminaKeepRate: Double,
    val averageStaminaKeepDistance: Double,
    val averagePositionCompetitionCount: Double,
    val competeFightFinishRate: Double,
    val averageCompeteFightTime: Double,
)

@Serializable
data class RaceRunOutput(
    val raceTime: Double,
    val raceTimeDelta: Double,
    val raceTimeWithoutRunUp: Double,
    val maxSpurt: Boolean,
    val spDiff: Double,
    val positionCompetitionCount: Int,
    val staminaKeepDistance: Double,
    val competeFightFinished: Boolean,
    val competeFightTime: Double,
)

@Serializable
data class RaceFrameOutput(
    val index: Int,
    val time: Double,
    val speed: Double,
    val sp: Double,
    val position: Double,
    val targetSpeed: Double,
    val acceleration: Double,
    val currentLane: Double,
    val triggeredSkills: List<String>,
    val endedSkills: List<String>,
    val triggeredDebuffs: List<String>,
)

@Serializable
data class DataDumpResponse(
    val type: String,
    val data: JsonElement,
)

@Serializable
data class TrackLocationOutput(
    val location: Int,
    val name: String,
    val courses: List<TrackCourseOutput>,
)

@Serializable
data class TrackCourseOutput(
    val course: Int,
    val detail: TrackDetail,
)

@Serializable
data class EventTrackOutput(
    val label: String,
    val location: Int,
    val course: Int,
    val condition: String,
    val conditionLabel: String,
    val gateCount: Int,
    val locationName: String,
    val courseName: String,
)

@Serializable
data class CharaOutput(
    val id: Int,
    val name: String,
    val charaId: Int,
    val charaName: String,
    val rarity: Int,
    val rank: Int,
    val speedBonus: Int,
    val staminaBonus: Int,
    val powerBonus: Int,
    val gutsBonus: Int,
    val wisdomBonus: Int,
    val initialStatus: StatusOutput,
    val imageColor: String,
)

@Serializable
data class StatusOutput(
    val speed: Int,
    val stamina: Int,
    val power: Int,
    val guts: Int,
    val wisdom: Int,
    val skillHints: List<String>,
)

@Serializable
data class EnumEntryOutput(
    val name: String,
    val label: String? = null,
    val value: Int? = null,
)

@Serializable
data class EnumDumpOutput(
    val condition: List<EnumEntryOutput>,
    val style: List<EnumEntryOutput>,
    val fitRank: List<String>,
    val courseCondition: List<EnumEntryOutput>,
    val skillActivateAdjustment: List<EnumEntryOutput>,
    val randomPosition: List<EnumEntryOutput>,
    val positionKeepMode: List<EnumEntryOutput>,
    val debuffType: List<EnumEntryOutput>,
)

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun main(args: Array<String>) {
    if (args.any { it == "--help" || it == "-h" }) {
        printHelp()
        return
    }
    try {
        val dataMode = parseDataMode(args)
        if (dataMode != null) {
            val dataDir = parseOption(args, "--data-dir") ?: "data"
            val response = dumpData(dataMode, File(dataDir))
            writeJson(response)
            return
        }
        val input = readInput(args)
        val request = json.decodeFromString<RaceCliRequest>(input)
        loadData(request)
        val response = simulate(request)
        writeJson(response)
    } catch (e: Exception) {
        System.err.println("race-cli failed: ${e.javaClass.name}: ${e.message ?: "(no message)"}")
        e.stackTraceToString().lineSequence().drop(1).take(8).forEach {
            System.err.println(it)
        }
        exitProcess(1)
    }
}

private inline fun <reified T> writeJson(value: T) {
    System.out.write(json.encodeToString(value).toByteArray(Charsets.UTF_8))
    System.out.write('\n'.code)
    System.out.flush()
}

private fun parseDataMode(args: Array<String>): String? {
    val index = args.indexOf("--data")
    if (index < 0) return null
    return args.getOrNull(index + 1)
        ?: throw IllegalArgumentException("--data requires a type: track, event-track, skill, chara, enum, or all")
}

private fun parseOption(args: Array<String>, name: String): String? {
    val index = args.indexOf(name)
    if (index < 0) return null
    return args.getOrNull(index + 1) ?: throw IllegalArgumentException("$name requires a value")
}

private fun readInput(args: Array<String>): String {
    val path = when {
        args.isEmpty() -> null
        args.size == 1 -> args[0]
        args.size == 2 && args[0] == "--input" -> args[1]
        else -> throw IllegalArgumentException("Usage: race-cli [input.json] or race-cli --input input.json")
    }
    return if (path == null || path == "-") {
        generateSequence(::readLine).joinToString("\n")
    } else {
        File(path).readText()
    }
}

private fun loadData(request: RaceCliRequest) {
    val dataDir = File(request.dataDir)
    val skillDataFile = request.skillDataPath?.let(::File) ?: File(dataDir, "skill_data.txt")
    val eventTrackFile = request.eventTrackPath?.let(::File) ?: File(dataDir, "event_track.txt")
    require(skillDataFile.isFile) {
        "skill data file not found: ${skillDataFile.absolutePath}. Set dataDir or skillDataPath in the request."
    }
    require(eventTrackFile.isFile) {
        "event track file not found: ${eventTrackFile.absolutePath}. Set dataDir or eventTrackPath in the request."
    }
    loadSkillDataFromString(skillDataFile.readText())
    loadRecentEventTrackListFromString(eventTrackFile.readText())
}

private fun dumpData(type: String, dataDir: File): DataDumpResponse {
    val normalizedType = type.lowercase()
    val data = when (normalizedType) {
        "track", "tracks" -> json.encodeToJsonElement(trackDump())
        "event-track", "event-tracks", "event_track", "event_tracks" -> {
            json.encodeToJsonElement(eventTrackDump(readDataFile(dataDir, "event_track.txt")))
        }
        "skill", "skills" -> {
            loadSkillDataFromString(readDataFile(dataDir, "skill_data.txt"))
            json.encodeToJsonElement(skillData2)
        }
        "chara", "charas", "character", "characters" -> {
            json.encodeToJsonElement(charaDump(readDataFile(dataDir, "chara.txt")))
        }
        "enum", "enums" -> json.encodeToJsonElement(enumDump())
        "all" -> json.encodeToJsonElement(
            mapOf(
                "track" to json.encodeToJsonElement(trackDump()),
                "eventTrack" to json.encodeToJsonElement(eventTrackDump(readDataFile(dataDir, "event_track.txt"))),
                "skill" to run {
                    loadSkillDataFromString(readDataFile(dataDir, "skill_data.txt"))
                    json.encodeToJsonElement(skillData2)
                },
                "chara" to json.encodeToJsonElement(charaDump(readDataFile(dataDir, "chara.txt"))),
                "enum" to json.encodeToJsonElement(enumDump()),
            )
        )
        else -> throw IllegalArgumentException(
            "Unknown data type: $type. Supported types: track, event-track, skill, chara, enum, all"
        )
    }
    return DataDumpResponse(normalizedType, data)
}

private fun readDataFile(dataDir: File, name: String): String {
    val file = File(dataDir, name)
    require(file.isFile) { "data file not found: ${file.absolutePath}" }
    return file.readText()
}

private fun trackDump(): List<TrackLocationOutput> {
    return trackData.entries.sortedBy { it.key }.map { (location, raceTrack) ->
        TrackLocationOutput(
            location = location,
            name = raceTrack.name,
            courses = raceTrack.courses.entries.sortedBy { it.key }.map { (course, detail) ->
                TrackCourseOutput(course, detail)
            },
        )
    }
}

private fun eventTrackDump(text: String): List<EventTrackOutput> {
    return buildList {
        text.lineSequence().forEach { line ->
            val data = line.trim().split(",")
            if (data.size < 4) return@forEach
            val month = data[0].toInt()
            val type = if (data[1] == "L") "リーグオブヒーローズ" else "チャンピオンズミーティング"
            val label = "${month}月$type"
            val courseName = data[2]
            val distance = data[3]
            val conditions = if (data.size < 5) CourseCondition.entries else {
                listOf(CourseCondition.valueOf(data[4]))
            }
            val gateCount = if (data[1] == "L") 12 else 9
            val location = trackData.entries.first { it.value.name == courseName }
            val course = location.value.courses.entries.first { it.value.name.startsWith(distance) }
            conditions.forEach { condition ->
                add(
                    EventTrackOutput(
                        label = label,
                        location = location.key,
                        course = course.key,
                        condition = condition.name,
                        conditionLabel = condition.label,
                        gateCount = gateCount,
                        locationName = location.value.name,
                        courseName = course.value.name,
                    )
                )
            }
        }
    }
}

private fun charaDump(text: String): List<CharaOutput> {
    return CharaLoader.load(text).map { chara ->
        CharaOutput(
            id = chara.id,
            name = chara.name,
            charaId = chara.charaId,
            charaName = chara.charaName,
            rarity = chara.rarity,
            rank = chara.rank,
            speedBonus = chara.speedBonus,
            staminaBonus = chara.staminaBonus,
            powerBonus = chara.powerBonus,
            gutsBonus = chara.gutsBonus,
            wisdomBonus = chara.wisdomBonus,
            initialStatus = StatusOutput(
                speed = chara.initialStatus.speed,
                stamina = chara.initialStatus.stamina,
                power = chara.initialStatus.power,
                guts = chara.initialStatus.guts,
                wisdom = chara.initialStatus.wisdom,
                skillHints = chara.initialStatus.skillHint.keys.toList(),
            ),
            imageColor = chara.imageColor,
        )
    }
}

private fun enumDump() = EnumDumpOutput(
    condition = Condition.entries.map { EnumEntryOutput(it.name, it.label, it.value) },
    style = Style.entries.map { EnumEntryOutput(it.name, it.text, it.value) },
    fitRank = FitRank.entries.map { it.name },
    courseCondition = CourseCondition.entries.map { EnumEntryOutput(it.name, it.label, it.value) },
    skillActivateAdjustment = SkillActivateAdjustment.entries.map { EnumEntryOutput(it.name, it.label, it.value) },
    randomPosition = RandomPosition.entries.map { EnumEntryOutput(it.name, it.label, it.value) },
    positionKeepMode = PositionKeepMode.entries.map { EnumEntryOutput(it.name, it.label) },
    debuffType = DebuffType.entries.map { EnumEntryOutput(it.name, it.label) },
)

private fun simulate(request: RaceCliRequest): RaceCliResponse {
    require(request.count > 0) { "count must be greater than 0" }
    val setting = request.toRaceSetting()
    val system = request.system.toSystemSetting()
    val threads = request.threads.coerceAtLeast(1)
    val results = mutableListOf<RaceSimulationResult>()
    val mutex = Mutex()
    var firstRunFrames: List<RaceFrame> = emptyList()
    runBlocking {
        List(threads) { index ->
            async(Dispatchers.Default) {
                val calculator = RaceCalculator(system)
                repeat(request.count / threads + if (index < request.count % threads) 1 else 0) {
                    val (result, state) = calculator.simulate(setting)
                    mutex.withLock {
                        if (firstRunFrames.isEmpty()) {
                            firstRunFrames = state.simulation.frames.toList()
                        }
                        results += result
                    }
                }
            }
        }.awaitAll()
    }
    return RaceCliResponse(
        count = results.size,
        summary = results.toSummary(),
        runs = if (request.includeRuns) results.map { it.toOutput() } else emptyList(),
        firstRunFrames = if (request.includeFirstRunFrames) {
            firstRunFrames.take(request.maxFrames ?: firstRunFrames.size).mapIndexed { index, frame ->
                frame.toOutput(index)
            }
        } else emptyList(),
    )
}

private fun RaceCliRequest.toRaceSetting(): RaceSetting {
    val default = RaceSetting()
    return RaceSetting(
        umaStatus = uma.toUmaStatus(default.umaStatus),
        track = track?.toTrack(default.track) ?: default.track,
        skillActivateAdjustment = setting.skillActivateAdjustment.toEnum(
            default.skillActivateAdjustment,
            SkillActivateAdjustment::label,
        ),
        randomPosition = setting.randomPosition.toEnum(default.randomPosition, RandomPosition::label),
        season = setting.season ?: default.season,
        weather = setting.weather ?: default.weather,
        badStart = setting.badStart ?: default.badStart,
        debuffCounts = DebuffType.entries.associateWith { type ->
            setting.debuffCounts[type.name] ?: setting.debuffCounts[type.label] ?: default.debuffCounts[type] ?: 0
        },
        positionKeepMode = setting.positionKeepMode.toEnum(default.positionKeepMode, PositionKeepMode::label),
        positionKeepRate = setting.positionKeepRate ?: default.positionKeepRate,
        virtualLeader = setting.virtualLeader?.toUmaStatus(default.virtualLeader) ?: default.virtualLeader,
    )
}

private fun UmaStatusInput.toUmaStatus(default: UmaStatus): UmaStatus {
    return UmaStatus(
        charaName = charaName ?: default.charaName,
        speed = speed ?: default.speed,
        stamina = stamina ?: default.stamina,
        power = power ?: default.power,
        guts = guts ?: default.guts,
        wisdom = wisdom ?: default.wisdom,
        condition = condition.toEnum(default.condition, Condition::label),
        style = style.toEnum(default.style, Style::text),
        distanceFit = distanceFit.toEnum(default.distanceFit),
        surfaceFit = surfaceFit.toEnum(default.surfaceFit),
        styleFit = styleFit.toEnum(default.styleFit),
        popularity = popularity ?: default.popularity,
        gateNumber = gateNumber ?: default.gateNumber,
        hasSkills = skills.map { it.toSkill() },
        uniqueLevel = uniqueLevel ?: default.uniqueLevel,
    )
}

private fun TrackInput.toTrack(default: Track): Track {
    return Track(
        location = location ?: default.location,
        course = course ?: default.course,
        condition = condition.toEnum(default.condition, CourseCondition::label),
        gateCount = gateCount ?: default.gateCount,
    )
}

private fun SystemSettingInput.toSystemSetting(): SystemSetting {
    val default = SystemSetting()
    return SystemSetting(
        skillLaneChangeRate = skillLaneChangeRate ?: default.skillLaneChangeRate,
        positionCompetitionRate = positionCompetitionRate ?: default.positionCompetitionRate,
        competeFightRate = competeFightRate ?: default.competeFightRate,
        secureLeadRate = secureLeadRate ?: default.secureLeadRate,
    )
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T, label: (T) -> String = { it.name }): T {
    if (this == null) return default
    return enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) || label(it) == this }
        ?: throw IllegalArgumentException("Unknown ${T::class.simpleName}: $this")
}

private fun String.toSkill(): SkillData {
    return findSkills(this)?.firstOrNull()
        ?: throw IllegalArgumentException("Unknown skill: $this")
}

private fun List<RaceSimulationResult>.toSummary(): RaceSummaryOutput {
    return RaceSummaryOutput(
        averageTime = averageOf { it.raceTime },
        bestTime = minOf { it.raceTime },
        worstTime = maxOf { it.raceTime },
        averageTimeWithoutRunUp = averageOf { it.raceTimeWithoutRunUp },
        averageTimeDelta = averageOf { it.raceTimeDelta },
        averageSp = averageOf { it.spDiff },
        bestSp = maxOf { it.spDiff },
        worstSp = minOf { it.spDiff },
        maxSpurtRate = count { it.maxSpurt } / size.toDouble(),
        staminaKeepRate = count { it.staminaKeepDistance > 0.0 } / size.toDouble(),
        averageStaminaKeepDistance = averageOf { it.staminaKeepDistance },
        averagePositionCompetitionCount = averageOf { it.positionCompetitionCount.toDouble() },
        competeFightFinishRate = count { it.competeFightFinished } /
                count { it.competeFightTime > 0.0 }.coerceAtLeast(1).toDouble(),
        averageCompeteFightTime = averageOf { it.competeFightTime },
    )
}

private fun <T> List<T>.averageOf(selector: (T) -> Double): Double = sumOf(selector) / size

private fun RaceSimulationResult.toOutput(): RaceRunOutput {
    return RaceRunOutput(
        raceTime = raceTime,
        raceTimeDelta = raceTimeDelta,
        raceTimeWithoutRunUp = raceTimeWithoutRunUp,
        maxSpurt = maxSpurt,
        spDiff = spDiff,
        positionCompetitionCount = positionCompetitionCount,
        staminaKeepDistance = staminaKeepDistance,
        competeFightFinished = competeFightFinished,
        competeFightTime = competeFightTime,
    )
}

private fun RaceFrame.toOutput(index: Int): RaceFrameOutput {
    return RaceFrameOutput(
        index = index,
        time = index / 15.0,
        speed = speed,
        sp = sp,
        position = startPosition,
        targetSpeed = targetSpeed,
        acceleration = acceleration,
        currentLane = currentLane,
        triggeredSkills = triggeredSkills.map { it.invoke.skill.name },
        endedSkills = endedSkills.map { it.data.skill.name },
        triggeredDebuffs = triggeredDebuffs.map { it.label },
    )
}

private fun printHelp() {
    println(
        """
        Usage: race-cli [input.json]
               race-cli --input input.json
               race-cli < input.json
               race-cli --data TYPE [--data-dir data]

        Minimal input:
        {"count":100,"uma":{"speed":1800,"stamina":1600,"power":1300,"guts":1200,"wisdom":1300}}

        By default dataDir is "data" and must contain skill_data.txt and event_track.txt.
        Enum fields accept Kotlin enum names, and Japanese labels where the simulator defines one.
        Data dump types: track, event-track, skill, chara, enum, all.
        """.trimIndent()
    )
}
