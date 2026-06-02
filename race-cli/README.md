# Race CLI

Small JSON command-line wrapper around the `:race` simulator for use from Python or other external tools.

Build:

```powershell
.\gradlew.bat :race-cli:fatJar
```

Run from the repository root, where `data/skill_data.txt` and `data/event_track.txt` are available:

```powershell
java -jar race-cli\build\libs\race-cli-1.0-all.jar input.json
```

Minimal input:

```json
{
  "count": 100,
  "threads": 1,
  "includeRuns": true,
  "uma": {
    "speed": 1800,
    "stamina": 1600,
    "power": 1300,
    "guts": 1200,
    "wisdom": 1300,
    "style": "NIGE",
    "condition": "BEST",
    "skills": ["右回り◎"]
  },
  "track": {
    "location": 10005,
    "course": 10507,
    "condition": "GOOD",
    "gateCount": 9
  }
}
```

Python example:

```python
import json
import subprocess

request = {
    "count": 100,
    "includeRuns": True,
    "uma": {"speed": 1800, "stamina": 1600, "power": 1300, "guts": 1200, "wisdom": 1300},
}

completed = subprocess.run(
    ["java", "-jar", r"race-cli\build\libs\race-cli-1.0-all.jar"],
    input=json.dumps(request),
    text=True,
    capture_output=True,
    check=True,
)
response = json.loads(completed.stdout)
print(response["summary"])
```

Enum fields accept Kotlin enum names such as `NIGE`, `BEST`, `S`, and also Japanese labels where the simulator defines them.
