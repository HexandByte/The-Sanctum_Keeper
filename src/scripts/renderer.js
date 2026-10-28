
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

    "open-folder-button":
        "Opening real folders is not available yet.",

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

// Update the dashboard with the selected folder.
function updateSourceFolderDisplay(folderPath) {
    const name = document.getElementById("source-folder-name");
    const path = document.getElementById("source-folder-path");
    const count = document.getElementById("stat-selected-folders");

    if (!folderPath) {
        name.textContent = "No folder selected";
        path.textContent = "Choose a folder to preview files.";
        count.textContent = "00";
        return;
    }

    // Extract the folder name from its full path.
    const normalizedPath = folderPath.replace(/[\\/]+$/, "");
    const folderName =
        normalizedPath.split(/[\\/]/).pop() || normalizedPath;

    name.textContent = folderName;
    path.textContent = folderPath;

    // Display the number of folders selected this session.
    count.textContent = String(selectedFolderPaths.size).padStart(2, "0");
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
            updateSourceFolderDisplay(folderPath);

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



"use strict";

// Stores the rules loaded from Java, in their original order.
let savedRules = [];
let editingRuleId = null;

// Create the rule manager when it is first opened.
function createRuleManager() {
    let modal = document.getElementById("rule-editor-modal");

    if (modal) {
        return modal;
    }

    modal = document.createElement("div");
    modal.id = "rule-editor-modal";
    modal.className = "rule-editor-modal";
    modal.hidden = true;

    // This is static interface markup; rule values are inserted as text.
    modal.innerHTML = `
        <div class="rule-editor-dialog"
             role="dialog"
             aria-modal="true"
             aria-labelledby="rule-editor-title">

            <div class="rule-editor-header">
                <div>
                    <p class="rule-editor-eyebrow">DETERMINISTIC ORGANIZATION</p>
                    <h2 id="rule-editor-title">Manage Rules</h2>
                </div>
                <button type="button"
                        id="rule-modal-close"
                        class="rule-modal-close"
                        aria-label="Close rule manager">×</button>
            </div>

            <p class="rule-editor-description">
                Rules run from top to bottom. The first matching rule wins.
            </p>

            <p id="rule-editor-message"
               class="rule-editor-message"
               role="status"
               aria-live="polite"></p>

            <section class="saved-rules-section">
                <h3>Saved Rules</h3>
                <div id="saved-rules-list" class="saved-rules-list"></div>
            </section>

            <form id="rule-editor-form" class="rule-editor-form">
                <h3 id="rule-form-title">Create a Rule</h3>

                <label class="rule-form-field">
                    Condition type
                    <select id="rule-condition-type" required>
                        <option value="extension">File extension</option>
                        <option value="filename">Filename</option>
                    </select>
                </label>

                <label id="rule-match-mode-field"
                       class="rule-form-field"
                       hidden>
                    Filename matching
                    <select id="rule-match-mode">
                        <option value="contains">Contains</option>
                        <option value="startsWith">Starts with</option>
                        <option value="equals">Equals</option>
                    </select>
                </label>

                <label class="rule-form-field">
                    Value
                    <input id="rule-condition-value"
                           type="text"
                           placeholder="pdf or invoice"
                           required>
                    <span id="rule-value-help" class="rule-field-help">
                        Enter an extension without the dot, such as pdf.
                    </span>
                </label>

                <label class="rule-form-field">
                    Destination folder
                    <input id="rule-destination"
                           type="text"
                           placeholder="Documents/PDFs"
                           required>
                    <span class="rule-field-help">
                        A relative path inside the selected source folder.
                    </span>
                </label>

                <div class="rule-form-actions">
                    <button id="rule-form-submit"
                            type="submit"
                            class="rule-primary-button">Save Rule</button>
                    <button id="rule-form-cancel"
                            type="button"
                            class="rule-secondary-button"
                            hidden>Cancel Edit</button>
                </div>
            </form>
        </div>
    `;

    document.body.appendChild(modal);

    const closeButton = modal.querySelector("#rule-modal-close");
    const form = modal.querySelector("#rule-editor-form");
    const conditionType = modal.querySelector("#rule-condition-type");
    const matchModeField = modal.querySelector("#rule-match-mode-field");
    const valueHelp = modal.querySelector("#rule-value-help");
    const cancelButton = modal.querySelector("#rule-form-cancel");

    closeButton.addEventListener("click", closeRuleManager);

    // Clicking the shaded background closes the modal.
    modal.addEventListener("click", function (event) {
        if (event.target === modal) {
            closeRuleManager();
        }
    });

    // Show filename options only when the selected condition needs them.
    conditionType.addEventListener("change", function () {
        const isFilename = conditionType.value === "filename";
        matchModeField.hidden = !isFilename;

        valueHelp.textContent = isFilename
            ? "Enter the filename text to match, without its extension."
            : "Enter an extension without the dot, such as pdf.";
    });

    cancelButton.addEventListener("click", resetRuleForm);
    form.addEventListener("submit", saveRuleFromForm);

    return modal;
}

// Open the modal and load the latest saved rules.
async function openRuleManager() {
    const modal = createRuleManager();
    modal.hidden = false;

    const message = modal.querySelector("#rule-editor-message");
    message.textContent = "Loading saved rules...";

    resetRuleForm();

    try {
        const data = await window.sanctum.getRules();

        savedRules = Array.isArray(data?.rules) ? data.rules : [];

        renderSavedRules();
        renderDashboardRules();

        message.textContent = savedRules.length
            ? `${savedRules.length} saved rule(s).`
            : "No saved rules yet. Create your first rule.";

    } catch (error) {
        console.error("Could not load rules:", error);
        message.textContent = error.message || "Could not load saved rules.";
        showNotification("Could not load rules from Java.");
    }
}

// Close the rule manager without changing saved rules.
function closeRuleManager() {
    const modal = document.getElementById("rule-editor-modal");

    if (modal) {
        modal.hidden = true;
    }

    resetRuleForm();
}

// Clear the form and return to create mode.
function resetRuleForm() {
    const modal = document.getElementById("rule-editor-modal");

    if (!modal) {
        return;
    }

    editingRuleId = null;

    modal.querySelector("#rule-editor-form").reset();
    modal.querySelector("#rule-form-title").textContent = "Create a Rule";
    modal.querySelector("#rule-form-submit").textContent = "Save Rule";
    modal.querySelector("#rule-form-cancel").hidden = true;
    modal.querySelector("#rule-match-mode-field").hidden = true;

    modal.querySelector("#rule-value-help").textContent =
        "Enter an extension without the dot, such as pdf.";
}

// Display the real rules returned by Java.
function renderSavedRules() {
    const modal = document.getElementById("rule-editor-modal");
    const list = modal.querySelector("#saved-rules-list");

    list.replaceChildren();

    if (savedRules.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.className = "rule-empty-message";
        emptyMessage.textContent = "No rules have been saved yet.";
        list.appendChild(emptyMessage);
        return;
    }

    savedRules.forEach(function (rule, index) {
        const item = document.createElement("article");
        item.className = "saved-rule-item";

        const details = document.createElement("div");
        details.className = "saved-rule-details";

        const title = document.createElement("strong");
        title.textContent = `${index + 1}. ${rule.id}`;

        const condition = rule.condition || {};
        const action = rule.action || {};

        const description = document.createElement("p");

        if (condition.type === "filename") {
            description.textContent =
                `Filename ${condition.matchMode || ""} ` +
                `"${condition.value || ""}" → ` +
                `${action.destination || ""}`;
        } else {
            description.textContent =
                `Extension "${condition.value || ""}" → ` +
                `${action.destination || ""}`;
        }

        const actions = document.createElement("div");
        actions.className = "saved-rule-actions";

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.textContent = "Edit";
        editButton.className = "rule-secondary-button";
        editButton.addEventListener("click", function () {
            editSavedRule(rule.id);
        });

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.textContent = "Remove";
        removeButton.className = "rule-remove-button";
        removeButton.addEventListener("click", function () {
            removeSavedRule(rule.id);
        });

        details.append(title, description);
        actions.append(editButton, removeButton);
        item.append(details, actions);
        list.appendChild(item);
    });
}

// Load a saved rule into the form for editing.
function editSavedRule(ruleId) {
    const rule = savedRules.find(item => item.id === ruleId);

    if (!rule) {
        return;
    }

    const modal = document.getElementById("rule-editor-modal");
    const condition = rule.condition || {};
    const action = rule.action || {};

    editingRuleId = rule.id;

    modal.querySelector("#rule-condition-type").value =
        condition.type || "extension";

    modal.querySelector("#rule-match-mode").value =
        condition.matchMode || "contains";

    modal.querySelector("#rule-condition-value").value =
        condition.value || "";

    modal.querySelector("#rule-destination").value =
        action.destination || "";

    modal.querySelector("#rule-match-mode-field").hidden =
        condition.type !== "filename";

    modal.querySelector("#rule-form-title").textContent = "Edit Rule";
    modal.querySelector("#rule-form-submit").textContent = "Save Changes";
    modal.querySelector("#rule-form-cancel").hidden = false;

    modal.querySelector("#rule-editor-message").textContent =
        `Editing ${rule.id}. Saving keeps its position in the rule order.`;

    modal.querySelector("#rule-editor-form").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}
// Display the saved Java rules in the dashboard.
function renderDashboardRules() {
    const list = document.getElementById("dashboard-rule-list");
    const footnote = document.getElementById("dashboard-rules-footnote");
    const count = document.getElementById("stat-active-rules");

    if (!list || !footnote || !count) {
        return;
    }

    list.replaceChildren();

    count.textContent = String(savedRules.length).padStart(2, "0");

    if (savedRules.length === 0) {
        const empty = document.createElement("p");
        empty.className = "rule-empty-message";
        empty.textContent = "No saved rules. Use Manage to create one.";

        list.appendChild(empty);
        footnote.textContent = "No saved rules";
        return;
    }

    savedRules.forEach(function (rule, index) {
        const condition = rule.condition || {};
        const action = rule.action || {};

        const item = document.createElement("article");
        item.className = "rule-item";

        const icon = document.createElement("div");
        icon.className = "rule-icon " +
            (condition.type === "extension" ? "rule-green" : "rule-blue");

        icon.textContent = condition.type === "extension"
            ? (condition.value || "FILE").toUpperCase().slice(0, 3)
            : "TXT";

        const details = document.createElement("div");
        details.className = "rule-details";

        const title = document.createElement("strong");
        title.textContent = condition.type === "extension"
            ? `${condition.value || "Unknown"} files`
            : `Filename rule ${index + 1}`;

        const description = document.createElement("span");
        description.textContent = condition.type === "filename"
            ? `${condition.matchMode || "contains"} "${condition.value || ""}"`
            : `.${condition.value || ""}`;

        const arrow = document.createElement("span");
        arrow.className = "rule-arrow";
        arrow.textContent = "→";

        const destination = document.createElement("span");
        destination.className = "rule-destination";
        destination.textContent = action.destination || "No destination";

        details.append(title, description);
        item.append(icon, details, arrow, destination);
        list.appendChild(item);
    });

    footnote.textContent =
        `${savedRules.length} saved rule(s) · First matching rule wins`;
}

// Validate the form and save the updated rule list through Java.
async function saveRuleFromForm(event) {
    event.preventDefault();

    const modal = document.getElementById("rule-editor-modal");
    const message = modal.querySelector("#rule-editor-message");

    const type = modal.querySelector("#rule-condition-type").value;
    const matchMode = modal.querySelector("#rule-match-mode").value;
    const value = modal.querySelector("#rule-condition-value").value.trim();
    const destination = modal.querySelector("#rule-destination").value.trim();

    if (!value || !destination) {
        message.textContent = "Enter both a condition value and destination.";
        return;
    }

    // Reject absolute paths and paths that try to leave the source folder.
    const absolutePath =
        /^[a-zA-Z]:[\\/]/.test(destination) ||
        /^[\\/]/.test(destination);

    const escapesSource = destination
        .split(/[\\/]+/)
        .some(part => part === "..");

    if (absolutePath || escapesSource) {
        message.textContent =
            "Use a relative destination inside the selected source folder.";
        return;
    }

    // Build a rule using the Java backend's existing JSON schema.
    const rule = {
        id: editingRuleId ||
            `rule-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        condition: {
            type,
            value,
            ...(type === "filename" ? { matchMode } : {})
        },
        action: {
            type: "move",
            destination
        }
    };

    // Replace a rule in place or append a new rule at the end.
    const updatedRules = editingRuleId
        ? savedRules.map(item => item.id === editingRuleId ? rule : item)
        : [...savedRules, rule];

    message.textContent = "Saving rule...";

    try {
        const result = await window.sanctum.saveRules(updatedRules);

        savedRules = updatedRules;
        updateSavedRulesCount();
        renderSavedRules();
        renderDashboardRules();
        resetRuleForm();

        message.textContent =
            `${result.savedCount} rule(s) saved successfully.`;

        showNotification("Rules saved successfully.");

    } catch (error) {
        console.error("Could not save rules:", error);

        message.textContent =
            error.message || "The backend could not save these rules.";
    }
}

// Remove a rule and save the remaining ordered list.
async function removeSavedRule(ruleId) {
    const updatedRules = savedRules.filter(rule => rule.id !== ruleId);
    const modal = document.getElementById("rule-editor-modal");
    const message = modal.querySelector("#rule-editor-message");

    message.textContent = "Saving updated rules...";

    try {
        const result = await window.sanctum.saveRules(updatedRules);

        savedRules = updatedRules;
        updateSavedRulesCount();
        renderSavedRules();
        renderDashboardRules();
        resetRuleForm();

        message.textContent =
            `${result.savedCount} rule(s) saved successfully.`;

        showNotification("Rule removed.");

    } catch (error) {
        console.error("Could not remove rule:", error);

        message.textContent =
            error.message || "Could not remove the rule.";
    }
}

// Connect the existing dashboard buttons to the rule manager.
document.getElementById("create-rule-button")
    ?.addEventListener("click", openRuleManager);

document.getElementById("manage-rules-button")
    ?.addEventListener("click", openRuleManager);

// Load the saved rule count from Java.
async function updateSavedRulesCount() {
    const count = document.getElementById("stat-active-rules");

    if (!count) return;

    try {
        const data = await window.sanctum.getRules();
        const rules = Array.isArray(data?.rules) ? data.rules : [];

        count.textContent = String(rules.length).padStart(2, "0");
    } catch (error) {
        console.error("Could not load saved rules:", error);
        count.textContent = "—";
    }
}

updateSavedRulesCount();

// Load saved rules when the dashboard opens.
async function loadDashboardRules() {
    try {
        const data = await window.sanctum.getRules();

        savedRules = Array.isArray(data?.rules) ? data.rules : [];

        renderDashboardRules();
    } catch (error) {
        console.error("Could not load dashboard rules:", error);

        const footnote = document.getElementById(
            "dashboard-rules-footnote"
        );

        if (footnote) {
            footnote.textContent = "Could not load rules from Java.";
        }
    }
}

loadDashboardRules();