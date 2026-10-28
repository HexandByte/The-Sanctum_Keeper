
"use strict";

const { contextBridge, ipcRenderer } = require("electron");

// Expose only approved operations to the frontend.
contextBridge.exposeInMainWorld("sanctum", {
    // Open the native folder picker.
    selectFolder: () => ipcRenderer.invoke("dialog:select-folder"),

    // Open a folder in Windows File Explorer.
    openFolder: (folderPath) =>
    ipcRenderer.invoke("folder:open", folderPath),

    // Load and save rules through Java.
    getRules: () => ipcRenderer.invoke("engine:get-rules"),
    saveRules: (rules) => ipcRenderer.invoke("engine:save-rules", rules),

    // Preview and organize files in the selected folder.
    preview: (sourceFolder) =>
        ipcRenderer.invoke("engine:preview", sourceFolder),

    organize: (sourceFolder) =>
        ipcRenderer.invoke("engine:organize", sourceFolder),

    // Undo the latest eligible file move.
    undo: () => ipcRenderer.invoke("engine:undo")
});
