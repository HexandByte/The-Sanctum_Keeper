
"use strict";

const { app, BrowserWindow, ipcMain, dialog, shell } = require("electron");
const path = require("node:path");
const fs = require("node:fs/promises");
const JavaBridge = require("./javaBridge");

let javaBridge = null;

// Create the application window with secure renderer settings.
function createWindow() {
    const win = new BrowserWindow({
        width: 1000,
        height: 700,
        title: "The Sanctum Keeper",

        webPreferences: {
            preload: path.join(__dirname, "preload.js"),
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    win.loadFile(path.join(__dirname, "index.html"));
}

// Open the native folder picker and return the selected path.
ipcMain.handle("dialog:select-folder", async () => {
    const result = await dialog.showOpenDialog({
        properties: ["openDirectory"]
    });

    if (result.canceled || result.filePaths.length === 0) {
        return null;
    }

    return result.filePaths[0];
});

// Open a selected directory in the system file manager.
ipcMain.handle("folder:open", async (_event, folderPath) => {
    if (typeof folderPath !== "string" || !folderPath.trim()) {
        throw new Error("A valid folder path is required.");
    }

    const resolvedPath = path.resolve(folderPath);

    const fs = require("node:fs/promises");
    const stats = await fs.stat(resolvedPath);

    if (!stats.isDirectory()) {
        throw new Error("The selected path is not a folder.");
    }

    const errorMessage = await shell.openPath(resolvedPath);

    if (errorMessage) {
        throw new Error(errorMessage);
    }

    return { opened: true };
});

// Load the saved rules from Java.
ipcMain.handle("engine:get-rules", () => {
    return javaBridge.send("getRules");
});

// Validate and save the supplied rules through Java.
ipcMain.handle("engine:save-rules", (_event, rules) => {
    return javaBridge.send("saveRules", { rules });
});

// Request a preview of the selected folder.
ipcMain.handle("engine:preview", (_event, sourceFolder) => {
    return javaBridge.send("preview", { sourceFolder });
});

// Organize files according to the saved Java rules.
ipcMain.handle("engine:organize", (_event, sourceFolder) => {
    return javaBridge.send("organize", { sourceFolder });
});

// Undo the most recent eligible file move.
ipcMain.handle("engine:undo", () => {
    return javaBridge.send("undo");
});

// Initialize the Java bridge and open the application.
app.whenReady().then(() => {
    javaBridge = new JavaBridge({
        jarPath: path.join(
            __dirname,
            "backend",
            "target",
            "sanctum-keeper-engine-1.0.0.jar"
        ),
        configDirectory: path.join(
            app.getPath("userData"),
            "config"
        )
    });

    createWindow();
});

// Stop Java when the application quits.
app.on("before-quit", () => {
    javaBridge?.stop();
});

// Follow the standard platform-specific window-closing behavior.
app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
        app.quit();
    }
});
