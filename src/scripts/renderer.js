
"use strict";

// ======================================== //
// 1. Select the HTML elements we need // 
// ======================================== // 

const previewButton = document.querySelector("#preview-button");
const runPreviewButton = document.querySelector("#run-preview-button");
const reviewButton = document.querySelector("#review-button");

const previewSummary = document.querySelector("#preview-summary");
const previewTableBody = document.querySelector("#preview-table-body");

const organizeButton = document.querySelector("#organize-button");
const navLinks = document.querySelectorAll(".nav-link");

// ========================================  //
// 2. Sample data for our dry-run preview //
// ======================================== //

// const sampleFiles = [
//     {
//         name: "invoice_march.pdf",
//         type: "PDF",
//         size: "342 KB",
//         destination: "Documents / Invoices",
//         icon: "PDF",
//         category: "pdf"
//     },
//     {
//         name: "wallpaper.png",
//         type: "PNG",
//         size: "2.4 MB",
//         destination: "Images / Wallpapers",
//         icon: "IMG",
//         category: "image"
//     },
//     {
//         name: "project-notes.md",
//         type: "MD",
//         size: "12 KB",
//         destination: "Documents / Notes",
//         icon: "TXT",
//         category: "text-file"
//     }
// ];

// ======================================== //
// 3. Show a notification //
// ======================================== //

function showNotification(message) {
    let notification = document.querySelector("#app-notification");

    if (!notification) {
        notification = document.createElement("div");
        notification.id = "app-notification";
        notification.className = "app-notification";
        notification.setAttribute("role", "status");
        notification.setAttribute("aria-live", "polite");

        document.body.appendChild(notification);
    }

    notification.textContent = message;
    notification.classList.add("visible");
}

// ======================================== //
// 4. Simulate an organization preview //
// ======================================== //


function getDestinationFilePath(destinationFolder, fileName) {
    // Java returns the destination folder, so append the filename.
    const separator = destinationFolder.includes("\\") ? "\\" : "/";
    const needsSeparator = !/[\\/]$/.test(destinationFolder);

    return destinationFolder +
        (needsSeparator ? separator : "") +
        fileName;
}

async function runPreview() {
    // The current Java backend previews one folder per request.
    if (selectedFolderPaths.size === 0) {
        showNotification("Please select a folder first.");
        return;
    }

    if (selectedFolderPaths.size > 1) {
        showNotification(
            "Please keep only one folder selected for this preview."
        );
        return;
    }

    const sourceFolder = selectedFolderPaths.values().next().value;

    // Show loading feedback and prevent execution during preview.
    previewButton.disabled = true;
    runPreviewButton.disabled = true;
    organizeButton.disabled = true;

    previewTableBody.replaceChildren();
    previewSummary.textContent = "Scanning selected folder...";

    try {
        // Ask Java to scan the folder and evaluate its saved rules.
        const data = await window.sanctum.preview(sourceFolder);
        const results = Array.isArray(data.results) ? data.results : [];

        results.forEach(function (file) {
            const row = document.createElement("tr");

            // File name and its small extension label.
            const nameCell = document.createElement("td");
            const nameWrapper = document.createElement("span");
            const fileIcon = document.createElement("span");

            nameWrapper.className = "file-name";
            fileIcon.className = "file-type";

            const extension = file.fileName.includes(".")
                ? file.fileName.split(".").pop()
                : "FILE";

            fileIcon.textContent = extension.toUpperCase().slice(0, 3);

            nameWrapper.append(
                fileIcon,
                document.createTextNode(file.fileName)
            );

            nameCell.appendChild(nameWrapper);

            // Show the status returned by Java.
            const statusCell = document.createElement("td");
            statusCell.textContent = file.status || "UNKNOWN";

            // Show the matched rule ID, if there is one.
            const ruleCell = document.createElement("td");
            ruleCell.textContent = file.ruleId || "—";

            // Java currently returns a destination folder, not a full file path.
            const destinationCell = document.createElement("td");
            const destination = document.createElement("span");

            destination.className = "destination";

            if (file.status === "MATCHED" && file.destination) {
                destination.textContent = getDestinationFilePath(
                    file.destination,
                    file.fileName
                );
            } else {
                destination.textContent = "No matching rule";
            }

            destinationCell.appendChild(destination);

            row.append(
                nameCell,
                statusCell,
                ruleCell,
                destinationCell
            );

            previewTableBody.appendChild(row);
        });

        // Use the actual summary returned by the Java backend.
        const summary = data.summary || {};
        const total = summary.totalFiles ?? results.length;
        const matched = summary.matchedFiles ?? 0;
        const unmatched = summary.unmatchedFiles ?? 0;

        previewSummary.textContent =
            `${total} files scanned · ${matched} matched · ` +
            `${unmatched} without a matching rule`;

        showNotification("Preview complete. No files were changed.");

    } catch (error) {
        console.error("Preview failed:", error);

        previewSummary.textContent = "Preview failed.";

        showNotification(
            error.message || "Could not preview the selected folder."
        );

    } finally {
        // Restore the preview buttons; real organization stays disabled.
        previewButton.disabled = false;
        runPreviewButton.disabled = false;
        organizeButton.disabled = true;
    }
}


// ======================================== //
// 5. Connect buttons to their actions //
// ======================================== //

previewButton.addEventListener("click", runPreview);
runPreviewButton.addEventListener("click", runPreview);

reviewButton.addEventListener("click", function () {
    document.querySelector("#folders").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });

    showNotification("You're reviewing the sample organization preview.");
});

// ======================================== //
// 6. Sidebar navigation //
// ======================================== //

navLinks.forEach(function (link) {
    link.addEventListener("click", function () {
        navLinks.forEach(function (item) {
            item.classList.remove("active");
        });

        link.classList.add("active");

        const sectionName = link.textContent.trim();

        showNotification("Selected " + sectionName + ".");
    });
});

// ======================================== //
// 7. Quick-action feedback //
// ======================================== //

const actionMessages = {
    "change-folder-button":
        "Folder selection is not connected yet.",

    "create-rule-button":
        "The rule editor is coming next.",

    "open-folder-button":
        "Opening real folders is not available yet.",

    "manage-rules-button":
        "Rule management is not connected yet.",

    "view-activity-button":
        "You're viewing the current sample activity.",

    "settings-button":
        "Workspace settings are not implemented yet."
};

Object.entries(actionMessages).forEach(function ([id, message]) {
    const button = document.getElementById(id);

    if (button) {
        button.addEventListener("click", function () {
            showNotification(message);
        });
    }
});

const addFolderButton = document.getElementById("add-folder-button");
const selectedFolderPaths = new Set();

function renderSelectedFolders() {
    let panel = document.getElementById("selected-folders-panel");

    if (!panel) {
        panel = document.createElement("section");
        panel.id = "selected-folders-panel";
        panel.className = "selected-folders-panel";

        panel.innerHTML = `
            <div class="selected-folders-heading">
                <h2>Selected Folders</h2>
                <p>Available for this session only</p>
            </div>
            <div id="selected-folders-list"
                 class="selected-folders-list"></div>
        `;

        const quickActions = document.querySelector(".quick-actions");

        if (quickActions) {
            quickActions.insertAdjacentElement("afterend", panel);
        } else if (addFolderButton?.parentElement) {
            addFolderButton.parentElement.insertAdjacentElement(
                "afterend",
                panel
            );
        } else {
            document.querySelector("main")?.appendChild(panel);
        }
    }

    const list = document.getElementById("selected-folders-list");
    list.replaceChildren();

    if (selectedFolderPaths.size === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.textContent = "No folders selected yet.";
        list.appendChild(emptyMessage);
        return;
    }

    selectedFolderPaths.forEach(function (folderPath) {
        const item = document.createElement("div");
        item.className = "selected-folder-item";

        const details = document.createElement("div");
        details.className = "selected-folder-details";

        const name = document.createElement("strong");
        const normalizedPath = folderPath.replace(/[\\/]+$/, "");

        name.textContent =
            normalizedPath.split(/[\\/]/).pop() || normalizedPath;

        const path = document.createElement("p");
        path.textContent = folderPath;

        details.append(name, path);

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.textContent = "Remove";
        removeButton.className = "remove-folder-button";

        removeButton.addEventListener("click", function () {
            selectedFolderPaths.delete(folderPath);
            renderSelectedFolders();
            showNotification("Folder removed from this session.");
        });

        item.append(details, removeButton);
        list.appendChild(item);
    });
}

if (addFolderButton) {
    addFolderButton.addEventListener("click", async function () {
        try {
            const folderPath = await window.sanctum.selectFolder();

            if (!folderPath) {
                showNotification("Folder selection cancelled.");
                return;
            }

            if (selectedFolderPaths.has(folderPath)) {
                showNotification("This folder is already selected.");
                return;
            }

            selectedFolderPaths.add(folderPath);
            renderSelectedFolders();

            showNotification("Folder selected successfully.");
        } catch (error) {
            console.error("Folder selection failed:", error);
            showNotification("Could not open the folder picker.");
        }
    });
}


// ========================================
// 8. Keep file operations disabled
// ========================================

organizeButton.disabled = true;

organizeButton.addEventListener("click", function () {
    showNotification(
        "Organization is disabled until the safe backend is implemented."
    );
});
