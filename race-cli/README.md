# Race CLI

Small JSON command-line wrapper around the `:race` simulator for use from Python or other external tools.

The CLI exposes the simulator knobs backed by `RaceSetting`, `UmaStatus`, `Track`, and `SystemSetting`. It does not currently expose frontend-only workflows such as contribution mode target selection or graph display toggles, although it can return the first run's raw frame trace for analysis.

## Build

```powershell
.\gradlew.bat :race-cli:fatJar
```

The standalone jar is written to:

```text
race-cli\build\libs\race-cli-1.0-all.jar
```

## Run

From the repository root:

```powershell
java -jar race-cli\build\libs\race-cli-1.0-all.jar input.json
```

Or via stdin:

```powershell
Get-Content input.json | java -jar race-cli\build\libs\race-cli-1.0-all.jar
```

The default `dataDir` is `data`, relative to the process working directory. If calling from another project, either set `cwd` to the umasim repo root or pass an absolute `dataDir`.

## Data Dump Mode

Use `--data` to dump simulator reference data as JSON without running a race:

```powershell
java -jar race-cli\build\libs\race-cli-1.0-all.jar --data track --data-dir data
```

The output shape is:

```json
{
  "type": "track",
  "data": []
}
```

Supported `--data` types:

| Type | Aliases | Data source | Meaning |
|---|---|---|---|
| `track` | `tracks` | `trackData` Kotlin table | All race locations and courses, including IDs and full `TrackDetail` course definitions. |
| `event-track` | `event-tracks`, `event_track`, `event_tracks` | `data/event_track.txt` plus `trackData` | Recent event courses resolved to numeric `location` and `course` IDs. |
| `skill` | `skills` | `data/skill_data.txt` | Full parsed skill table used by the simulator. |
| `chara` | `charas`, `character`, `characters` | `data/chara.txt` | Parsed character variants, bonuses, and initial statuses. |
| `enum` | `enums` | Kotlin enums | Accepted enum names, labels, and numeric values where available. |
| `all` | none | all of the above | One object containing `track`, `eventTrack`, `skill`, `chara`, and `enum`. |

`track` entries look like:

```json
{
  "location": 10005,
  "name": "東京",
  "courses": [
    {
      "course": 10507,
      "detail": {
        "name": "芝 2400m",
        "distance": 2400,
        "surface": "芝"
      }
    }
  ]
}
```

`event-track` entries are flattened for immediate simulation input:

```json
{
  "label": "6月チャンピオンズミーティング",
  "location": 10005,
  "course": 10507,
  "condition": "GOOD",
  "conditionLabel": "良",
  "gateCount": 9,
  "locationName": "東京",
  "courseName": "芝 2400m"
}
```

The JSON is UTF-8. If Japanese text displays as `?` in a Windows shell, parse stdout from Python or switch the terminal to UTF-8; the data itself is still JSON.

## Python Example

```python
import json
import subprocess
from pathlib import Path

repo_root = Path(r"F:\Programming\Games\umasim")
jar_path = repo_root / r"race-cli\build\libs\race-cli-1.0-all.jar"

request = {
    "count": 100,
    "includeRuns": True,
    "dataDir": str(repo_root / "data"),
    "uma": {
        "speed": 1800,
        "stamina": 1600,
        "power": 1300,
        "guts": 1200,
        "wisdom": 1300,
    },
}

completed = subprocess.run(
    ["java", "-jar", str(jar_path)],
    input=json.dumps(request, ensure_ascii=False),
    text=True,
    capture_output=True,
)
if completed.returncode:
    raise RuntimeError(
        f"race-cli failed with exit code {completed.returncode}\n"
        f"stdout:\n{completed.stdout}\n"
        f"stderr:\n{completed.stderr}"
    )

response = json.loads(completed.stdout)
print(response["summary"])
```

## Full Input Shape

All fields are optional unless noted. Unknown JSON fields are ignored.

```json
{
  "count": 100,
  "threads": 1,
  "dataDir": "data",
  "skillDataPath": null,
  "eventTrackPath": null,
  "includeRuns": true,
  "includeTimeCounts": false,
  "includeFirstRunFrames": false,
  "maxFrames": null,
  "uma": {},
  "track": null,
  "setting": {},
  "system": {}
}
```

### Top-Level Fields

| Field | Type | Default | Meaning |
|---|---:|---:|---|
| `count` | int | `1` | Number of race simulations to run. Must be greater than 0. |
| `threads` | int | `1` | Kotlin-side parallelism inside one CLI process. Use `1` if you plan to scale with Python multiprocessing. |
| `dataDir` | string | `"data"` | Directory containing `skill_data.txt` and `event_track.txt`. |
| `skillDataPath` | string/null | null | Explicit path to `skill_data.txt`; overrides `dataDir`. |
| `eventTrackPath` | string/null | null | Explicit path to `event_track.txt`; overrides `dataDir`. |
| `includeRuns` | boolean | `true` | Include one result object per simulation in `runs`. Disable for large `count` if only aggregate stats are needed. |
| `includeTimeCounts` | boolean | `false` | Include `timeCounts`, a memory-cheap race-time histogram keyed by rounded race time in milliseconds. |
| `includeFirstRunFrames` | boolean | `false` | Include the frame trace for the first completed run. Useful for plotting speed/stamina/skill timing. |
| `maxFrames` | int/null | null | Limit `firstRunFrames` length. Ignored unless `includeFirstRunFrames` is true. |
| `uma` | object | `{}` | Main runner status and skills. |
| `track` | object/null | null | Course/condition/gate settings. If null, uses first entry from `event_track.txt`. |
| `setting` | object | `{}` | Race-level simulation knobs. |
| `system` | object | `{}` | Low-level probability knobs. |

### `uma`

Maps to `UmaStatus`.

```json
{
  "charaName": "(未選択)",
  "speed": 1800,
  "stamina": 1600,
  "power": 1300,
  "guts": 1200,
  "wisdom": 1300,
  "condition": "BEST",
  "style": "NIGE",
  "distanceFit": "S",
  "surfaceFit": "A",
  "styleFit": "A",
  "popularity": 1,
  "gateNumber": 0,
  "skills": ["右回り◎", 901121, "200252"],
  "uniqueLevel": 6
}
```

| Field | Type | Default | Meaning |
|---|---:|---:|---|
| `charaName` | string | `"(未選択)"` | Character name. Mostly used for skill import/identity; formulas usually use raw stats and skills. |
| `speed` | int | `1800` | Speed stat. |
| `stamina` | int | `1600` | Stamina stat. |
| `power` | int | `1300` | Power stat. |
| `guts` | int | `1200` | Guts stat. |
| `wisdom` | int | `1300` | Wisdom stat. |
| `condition` | enum | `BEST` | Mood/condition. |
| `style` | enum | `NIGE` | Running style. |
| `distanceFit` | enum | `S` | Distance aptitude. |
| `surfaceFit` | enum | `A` | Surface aptitude. |
| `styleFit` | enum | `A` | Style aptitude. |
| `popularity` | int | `1` | Popularity rank. Used by some skill conditions. |
| `gateNumber` | int | `0` | Gate number control. `0` means random, `-1` biases inside, `-2` biases outside, positive values select a gate. |
| `skills` | string/int[] | `[]` | Skill IDs or names. Exact skill ID matches are preferred, then names are matched exactly/fuzzily like the frontend/MCP tool. Use IDs when duplicate skill names exist. |
| `uniqueLevel` | int | `6` | Unique skill level used when a unique skill is present. |

### `track`

Maps to `Track`.

```json
{
  "location": 10005,
  "course": 10507,
  "condition": "GOOD",
  "gateCount": 9
}
```

| Field | Type | Default | Meaning |
|---|---:|---:|---|
| `location` | int | from `event_track.txt` | Racecourse/location id. |
| `course` | int | from `event_track.txt` | Course id within the location. |
| `condition` | enum | from `event_track.txt` | Track condition. |
| `gateCount` | int | from `event_track.txt` | Number of gates/runners used for gate/post calculations. |

Course IDs are the same IDs used by `trackData` in the `race` module. Discover them with `--data track`, or use `--data event-track` for the recent event courses already resolved to simulation-ready IDs.

### `setting`

Maps to `RaceSetting` fields outside `uma` and `track`.

```json
{
  "skillActivateAdjustment": "NONE",
  "randomPosition": "RANDOM",
  "season": 0,
  "weather": 0,
  "badStart": false,
  "debuffCounts": {
    "Kensei": 0,
    "焦り": 1
  },
  "positionKeepMode": "APPROXIMATE",
  "positionKeepRate": 100,
  "virtualLeader": {
    "style": "NIGE",
    "skills": ["先手必勝", "大逃げ"]
  }
}
```

| Field | Type | Default | Meaning |
|---|---:|---:|---|
| `skillActivateAdjustment` | enum | `NONE` | Skill/random activation mode. `ALL` also sets `fixRandom` internally. |
| `randomPosition` | enum | `RANDOM` | Approximation setting for random skill activation positions. |
| `season` | int | `0` | Season flag used by some skill conditions. |
| `weather` | int | `0` | Weather flag used by some skill conditions. |
| `badStart` | boolean | `false` | Bad start flag used by some start-related conditions. |
| `debuffCounts` | object | all zero | Map of debuff enum name or Japanese label to count. |
| `positionKeepMode` | enum | `APPROXIMATE` | Position keep model. |
| `positionKeepRate` | int | `100` | Position keep trigger rate for `SPEED_UP` mode. |
| `virtualLeader` | `uma` object | default `UmaStatus` | Pace-maker runner used when `positionKeepMode` is `VIRTUAL`. |

### `system`

Maps to `SystemSetting`.

```json
{
  "skillLaneChangeRate": 0.4,
  "positionCompetitionRate": 0.8,
  "competeFightRate": 0.4,
  "secureLeadRate": 0.3
}
```

| Field | Type | Default | Meaning |
|---|---:|---:|---|
| `skillLaneChangeRate` | double | `0.4` | Probability used for skill-driven lane movement. |
| `positionCompetitionRate` | double | `0.8` | Probability for position competition. |
| `competeFightRate` | double | `0.4` | Probability for compete fight. |
| `secureLeadRate` | double | `0.3` | Probability for secure lead. |

## Enum Values

Enum inputs accept Kotlin enum names. For enums that define Japanese UI labels, those labels are also accepted.

### `condition`

| Name | Label |
|---|---|
| `BEST` | `絶好調` |
| `GOOD` | `好調` |
| `NORMAL` | `普通` |
| `BAD` | `不調` |
| `WORST` | `絶不調` |

### `style`

| Name | Label |
|---|---|
| `NIGE` | `逃げ` |
| `SEN` | `先行` |
| `SASI` | `差し` |
| `OI` | `追込` |
| `OONIGE` | `大逃げ` |

### Fit Ranks

`S`, `A`, `B`, `C`, `D`, `E`, `F`, `G`

### `track.condition`

| Name | Label |
|---|---|
| `GOOD` | `良` |
| `YAYAOMO` | `稍重` |
| `OMO` | `重` |
| `BAD` | `不良` |

### `skillActivateAdjustment`

| Name | Label | Meaning |
|---|---|---|
| `NONE` | `無` | Normal random activation. |
| `YES` | `確定発動` | Skills activate, but other race randomness remains. |
| `ALL` | `全乱数固定` | Skills activate and `fixRandom` becomes true. Useful for deterministic-ish comparisons. |

### `randomPosition`

| Name | Label |
|---|---|
| `RANDOM` | `ランダム` |
| `FASTEST` | `最速` |
| `FAST` | `1/4` |
| `MIDDLE` | `中間` |
| `SLOW` | `3/4` |
| `SLOWEST` | `最遅` |

### `positionKeepMode`

| Name | Label |
|---|---|
| `APPROXIMATE` | `近似` |
| `VIRTUAL` | `仮想ペースメーカー` |
| `SPEED_UP` | `一定確率でスピードアップ(逃げ)` |
| `NONE` | `無し` |

### Debuff Keys

`debuffCounts` accepts either enum names or Japanese labels.

| Name | Label |
|---|---|
| `Kensei` | `けん制` |
| `Aseri` | `焦り` |
| `NukegakeGold` | `逃亡禁止令` |
| `Nukegake` | `抜け駆け禁止` |
| `SasayakiGold` | `魅惑のささやき` |
| `Sasayaki` | `ささやき` |
| `StaminaEaterGold` | `スタミナグリード` |
| `StaminaEater` | `スタミナイーター` |
| `GankouGold` | `八方にらみ` |
| `Gankou` | `鋭い眼光` |
| `TrickGold` | `見惚れるトリック` |
| `Trick` | `トリック（前/後）` |
| `DrainForRose` | `Drain for rose（本体）` |
| `DrainForRose2` | `Drain for rose（継承）` |
| `Gorushi` | `Adventure of 564+金スタデバ` |

## Output Shape

```json
{
  "count": 100,
  "summary": {
    "averageTime": 123.45,
    "bestTime": 122.9,
    "worstTime": 124.1,
    "averageTimeWithoutRunUp": 123.45,
    "averageTimeDelta": 0.12,
    "averageSp": 100.0,
    "bestSp": 150.0,
    "worstSp": 50.0,
    "maxSpurtRate": 1.0,
    "staminaKeepRate": 0.0,
    "averageStaminaKeepDistance": 0.0,
    "averagePositionCompetitionCount": 1.2,
    "competeFightFinishRate": 0.5,
    "averageCompeteFightTime": 1.3
  },
  "runs": [],
  "timeCounts": {},
  "firstRunFrames": []
}
```

### `runs`

Included when `includeRuns` is true. Each item contains:

| Field | Meaning |
|---|---|
| `raceTime` | Full simulated race time. |
| `raceTimeDelta` | Delta against the course baseline used by the simulator. |
| `raceTimeWithoutRunUp` | Race time excluding run-up. |
| `maxSpurt` | Whether max spurt was achieved. |
| `spDiff` | Remaining stamina difference at spurt calculation. |
| `positionCompetitionCount` | Number of position competition events. |
| `staminaKeepDistance` | Distance spent in stamina keep mode. |
| `competeFightFinished` | Whether compete fight was still active at finish. |
| `competeFightTime` | Duration of compete fight. |

### `timeCounts`

Included when `includeTimeCounts` is true. Keys are rounded race times in milliseconds, and values are the number of simulations that finished at that time.

```json
{
  "116508": 3,
  "116556": 5,
  "116603": 2
}
```

This is intended for large `count` runs where `includeRuns` would return too much data.

### `firstRunFrames`

Included when `includeFirstRunFrames` is true. Each frame contains:

| Field | Meaning |
|---|---|
| `index` | Frame index. The simulator runs at 15 frames per second. |
| `time` | `index / 15.0`. |
| `speed` | Current speed. |
| `sp` | Current stamina. |
| `position` | Start position for the frame. |
| `targetSpeed` | Target speed used in the frame. |
| `acceleration` | Acceleration used in the frame. |
| `currentLane` | Current lane offset. |
| `triggeredSkills` | Skill names triggered this frame. |
| `endedSkills` | Skill names ended this frame. |
| `triggeredDebuffs` | Debuff labels triggered this frame. |

## Advanced Examples

### Deterministic Skill Position Comparison

```json
{
  "count": 1,
  "includeRuns": true,
  "includeFirstRunFrames": true,
  "setting": {
    "skillActivateAdjustment": "ALL",
    "randomPosition": "MIDDLE"
  },
  "uma": {
    "speed": 1800,
    "stamina": 1600,
    "power": 1300,
    "guts": 1200,
    "wisdom": 1300,
    "style": "SASI",
    "skills": ["右回り◎", "好転一息"]
  }
}
```

### Virtual Leader Position Keep

```json
{
  "count": 1000,
  "includeRuns": false,
  "threads": 4,
  "setting": {
    "positionKeepMode": "VIRTUAL",
    "virtualLeader": {
      "style": "NIGE",
      "skills": ["先手必勝", "大逃げ"]
    }
  },
  "uma": {
    "style": "OI",
    "speed": 1800,
    "stamina": 1600,
    "power": 1300,
    "guts": 1200,
    "wisdom": 1300
  }
}
```

## Current Gaps Compared With Frontend

The core race simulation knobs are exposed. These frontend workflows are not yet implemented in the CLI:

- Skill contribution modes (`CONTRIBUTION`, `CONTRIBUTION2`).
- Frontend graph display toggles. Use `includeFirstRunFrames` and plot in Python instead.
- Course lookup by human-readable course name. Use numeric `location` and `course` IDs for now.
- Explicit skill selection by skill id or rarity when duplicate names exist. The CLI currently selects the first fuzzy/name match.
