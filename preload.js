
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('sanctum', {
    selectFolder: () => ipcRenderer.invoke('dialog:select-folder')
});
