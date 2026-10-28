# The Sanctum Keeper

**The Sanctum Keeper** is a local-first desktop file organizer. It uses user-defined rules to preview and move files, with the Java engine responsible for rule matching and file operations.

> **MVP scope:** Select one source folder at a time, load or edit rules, preview proposed changes, organize files after confirmation, and undo eligible moves during the current Java-engine session.

## Features

- **Rule-based organization:** deterministic rules; the first matching rule wins.
- **Folder selection:** choose a source folder using the native operating-system folder picker.
- **Dry-run preview:** inspect matching and unmatched files before moving anything.
- **Conflict handling:** existing destination files are not intentionally overwritten; conflicts are reported.
- **Per-file results:** organization reports successful moves, unmatched files, conflicts, and failures.
- **Undo:** restore one eligible file move per request while the Java engine's in-memory history remains available.
- **Local configuration:** rules are stored in a `rules.json` file in the application's configuration directory.

The application processes **one selected source folder per preview or organization operation**. It does not continuously monitor folders in the background and does not recursively scan subfolders.

## Technology stack

- **Frontend:** HTML, CSS, JavaScript
- **Desktop shell:** Electron
- **Backend engine:** Java 21
- **Build tool:** Apache Maven
- **JSON serialization:** Gson

## Requirements

Install the following tools on your development machine:

- Node.js and npm
- JDK 21
- Apache Maven

The backend's `pom.xml` targets Java 21 and configures Maven Shade Plugin to create an executable JAR.

## Project structure

```text
The-Sanctum_Keeper/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/sanctumkeeper/
│       ├── Main.java
│       ├── model/
│       ├── organize/
│       ├── preview/
│       ├── scanner/
│       ├── storage/
│       └── validation/
├── src/
│   ├── scripts/renderer.js
│   └── styles/style.css
├── index.html
├── main.js
├── preload.js
├── javaBridge.js
├── package.json
└── README.md
```

Generated files, such as the packaged Java JAR, appear under `backend/target/` after a Maven build.

## Setup and run on Windows

Run commands from the repository root in PowerShell.

### 1. Install frontend dependencies

```powershell
npm.cmd install
```

If the repository has a valid `package-lock.json`, use `npm.cmd ci` for a clean dependency installation instead.

### 2. Build the Java backend

```powershell
mvn -f .\backend\pom.xml clean package
```

The expected executable JAR is:

```text
backend/target/sanctum-keeper-engine-1.0.0.jar
```

The build also creates an `original-...jar` as part of the shading process; the Electron bridge should launch the executable shaded JAR, not the `original-...jar`.

### 3. Run the desktop application

```powershell
npm.cmd start
```

The Electron main process launches the Java engine when the first backend command is requested. The Java process receives a configuration-directory path as its command-line argument.

> Ensure the terminal running Electron uses JDK 21. If Java 21 is installed but the current terminal selects another version, configure `JAVA_HOME`/`PATH` for that terminal before starting the app.

## Rules

Rules are ordered, and **First Spell Wins**: the first matching rule is selected for a file. If the selected rule's action fails for that file, the engine reports the failure rather than trying a later rule for the same file.

The current MVP supports these condition types:

- **Extension:** case-insensitive extension matching, such as `pdf` or `jpg`.
- **Filename:** case-insensitive matching against the filename without its extension. Supported modes are `startsWith`, `contains`, and `equals`.

The supported action is `move`. The destination must be a relative directory inside the selected source folder.

### Example rule

```json
{
  "id": "jpg-to-images",
  "condition": {
    "type": "extension",
    "value": "jpg"
  },
  "action": {
    "type": "move",
    "destination": "Images"
  }
}
```

This rule matches `.jpg` files and proposes moving them into an `Images` directory under the selected source folder.

## How it works

1. Select one source folder.
2. Load or save rules through the Java engine.
3. Run **Preview Organization** to see the proposed actions. Preview does not move files.
4. Review the results and confirm before choosing **Organize Files**.
5. Review actual per-file outcomes in the activity timeline.
6. Use **Undo last move** to ask Java to restore one eligible move.

The frontend presents results from Java; it does not independently implement the rule-matching or file-moving logic.

## Electron ↔ Java communication

Electron keeps the Java engine in a child process and communicates through newline-delimited JSON over standard input and output (NDJSON).

- Java prints a startup readiness message: `{"type":"ready","protocolVersion":1}`.
- Each request includes an `id` and `command`.
- The response echoes the request `id` and uses `ok: true` with `data`, or `ok: false` with a structured `error`.
- Standard output is reserved for JSON protocol messages. Diagnostic output belongs on standard error.

The current commands are:

| Command | Purpose | Required request data |
|---|---|---|
| `getRules` | Load saved rules | None beyond `id` and `command` |
| `saveRules` | Validate and save rules | `rules` array |
| `preview` | Preview files in a folder | `sourceFolder` string |
| `organize` | Move matching files in a folder | `sourceFolder` string |
| `undo` | Restore one recorded move | None beyond `id` and `command` |

Command-specific fields are at the top level of each request, as expected by the current Java `Main` class.

## Safety and current limitations

- Preview is non-mutating; file changes occur only through the organization command after user confirmation.
- The engine rejects source-folder symbolic links and rejects symbolic-link files during organization.
- Destination paths must remain inside the selected source folder.
- Existing destination files are reported as conflicts instead of being intentionally overwritten.
- The scanner checks files directly inside the selected folder. It skips hidden entries and does not recurse into subdirectories.
- Preview and organization can differ for symbolic-link entries because scanning may encounter entries that organization later rejects.
- Undo history is held in memory by the Java engine. It is not persistent across engine restarts, and each undo request restores at most one recorded move.
- Only one source folder is supported per preview or organization operation in the current MVP.
- Size-based rules, compound AND/OR/NOT rules, scheduled automation, arbitrary scripts, and continuous background folder monitoring are not part of the current MVP.

Use temporary folders and disposable test files when validating file operations.

## Development notes

- `main.js` owns the Electron window, native dialogs, IPC handlers, and Java bridge lifecycle.
- `preload.js` exposes a narrow API to the renderer using `contextBridge`.
- `javaBridge.js` starts the Java process, waits for readiness, sends commands, parses JSON lines, and matches replies to requests.
- `src/scripts/renderer.js` manages the dashboard and calls the allowed APIs exposed by preload.
- Java is the source of truth for rule matching, preview results, organization outcomes, and undo behavior.

## License

No license has been specified for this repository yet.
