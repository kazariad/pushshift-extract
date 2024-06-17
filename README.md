## pushshift-extract

A Java CLI for quickly decompressing and filtering Pushshift reddit archives with basic regex.

### Build
1. Clone repo.
2. Install [Java 21+](https://adoptium.net/).
3. Add the JDK's `bin` directory to your PATH.
4. Within the repo directory, execute `./mvnw.cmd package` on Windows, or `./mvnw package` on Linux/Mac.
5. The `pushshift-extract.jar` artifact will be located in the `target` directory.

### Usage
`java -jar pushshift-extract.jar -i inputPath [-o outputPath] [-r regex]`

Example:

`java -jar pushshift-extract.jar -i "D:\Downloads\reddit\submissions\RS_2024-01.zst" -r '.*\"subreddit\": \"nba\".*'`

Will decompress and save all submissions from the "nba" subreddit to `RS_2024-01.ndjson`.

### Useful links
[Pushshift torrent downloads](https://academictorrents.com/browse.php?search=Watchful1%2C+RaiderBDev)

[Pushshift subreddit](https://www.reddit.com/r/pushshift/)

[Web UI](https://arctic-shift.photon-reddit.com/)

[Python scripts](https://github.com/Watchful1/PushshiftDumps)
