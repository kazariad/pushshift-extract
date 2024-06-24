## pushshift-extract

A Java CLI for quickly decompressing and filtering Pushshift reddit archives with regex or JavaScript/Python scripting.

### Build
1. Clone repo.
2. Install [Java 21+](https://adoptium.net/).
3. Add the JDK's `bin` directory to your PATH.
4. Within the repo directory, execute `./mvnw.cmd package` on Windows, or `./mvnw package` on Linux/Mac.
5. The `pushshift-extract.jar` artifact will be located in the `target` directory.

### Usage
`java -jar pushshift-extract.jar -i inputPath [-o outputPath] [-j jsScriptPath | -p pyScriptPath | -r regex]`

JavaScript and Python script files must implement a `myFilter` function which accepts a string argument and returns a boolean indicating whether the line passes the filter and should be saved (true) or not (false).  JavaScript files should be written for ECMAScript 2023 and can use built-in functions, however modules aren't supported.  Python files should be written for Python 3 and can use built-in packages, however external modules/packages aren't supported.  Scripts should be stateless and not rely on persisting data between invocations.

### Examples
Find all submissions from the "nba" subreddit (regex):

`java -jar pushshift-extract.jar -i "D:\submissions\RS_2024-01.zst" -o "D:\output\nba.ndjson" -r '.*\"subreddit\": \"nba\".*'`

---
Find all submissions from the "nba" subreddit, created between 2024-01-07 and 2024-01-14, with an embedded video (JavaScript):

`java -jar pushshift-extract.jar -i "D:\submissions\RS_2024-01.zst" -o "D:\output\nba.ndjson" -j "D:\script.js"`

```JavaScript
// script.js
function myFilter(line) {
    let obj = JSON.parse(line);

    if (obj.subreddit.toLowerCase() !== 'nba') {
        return false;
    }

    let created = new Date(obj.created_utc * 1000);
    if (created < Date.UTC(2024, 0, 7) || created >= Date.UTC(2024, 0, 14)) {
        return false;
    }

    if (obj.media === null || obj.media.reddit_video === undefined) {
        return false;
    }

    return true;
}
```

---
Find all submissions from the "nba" subreddit, created between 2024-01-07 and 2024-01-14, with an embedded video (Python):

`java -jar pushshift-extract.jar -i "D:\submissions\RS_2024-01.zst" -o "D:\output\nba.ndjson" -p "D:\script.py"`

```Python
# script.py
import json
from datetime import datetime, timezone

def myFilter(line):
    obj = json.loads(line)

    if obj["subreddit"].lower() != "nba":
        return False

    created = datetime.fromtimestamp(obj["created_utc"], timezone.utc)
    if created < datetime(2024, 1, 7, tzinfo=timezone.utc) or created >= datetime(2024, 1, 14, tzinfo=timezone.utc):
        return False

    if obj["media"] is None or "reddit_video" not in obj["media"]:
        return False

    return True
```

### Benchmarks
The source archive used in the examples has a size of 16 GiB (197 GiB decompressed).  The program was run on a system with an AMD 5900X CPU, 32GB memory, and a 7200RPM HDD.

```
Regex:      7 min, 2 sec
JavaScript: 7 min, 49 sec
Python:     9 min, 45 sec
```


### Useful links
[Pushshift torrent downloads](https://academictorrents.com/browse.php?search=Watchful1%2C+RaiderBDev)

[Pushshift subreddit](https://www.reddit.com/r/pushshift/)

[Web UI](https://arctic-shift.photon-reddit.com/)

[Python scripts](https://github.com/Watchful1/PushshiftDumps)
