
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

const sampleFiles = [
    {
        name: "invoice_march.pdf",
        type: "PDF",
        size: "342 KB",
        destination: "Documents / Invoices",
        icon: "PDF",
        category: "pdf"
    },
    {
        name: "wallpaper.png",
        type: "PNG",
        size: "2.4 MB",
        destination: "Images / Wallpapers",
        icon: "IMG",
        category: "image"
    },
    {
        name: "project-notes.md",
        type: "MD",
        size: "12 KB",
        destination: "Documents / Notes",
        icon: "TXT",
        category: "text-file"
    }
];

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

function runPreview() {
    previewTableBody.replaceChildren();

    sampleFiles.forEach(function (file) {
        const row = document.createElement("tr");

        const nameCell = document.createElement("td");
        const nameWrapper = document.createElement("span");
        const fileIcon = document.createElement("span");

        nameWrapper.className = "file-name";
        fileIcon.className = "file-type " + file.category;
        fileIcon.textContent = file.icon;

        nameWrapper.append(fileIcon, document.createTextNode(file.name));
        nameCell.appendChild(nameWrapper);

        const typeCell = document.createElement("td");
        typeCell.textContent = file.type;

        const sizeCell = document.createElement("td");
        sizeCell.textContent = file.size;

        const destinationCell = document.createElement("td");
        const destination = document.createElement("span");

        destination.className = "destination";
        destination.textContent = file.destination;
        destinationCell.appendChild(destination);

        row.append(
            nameCell,
            typeCell,
            sizeCell,
            destinationCell
        );

        previewTableBody.appendChild(row);
    });

    previewSummary.textContent =
        sampleFiles.length + " sample files would be organized";

    organizeButton.disabled = true;

    showNotification(
        "Preview complete. Sample data only — no files were changed."
    );
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

    "add-folder-button":
        "Adding watched folders will be implemented next.",

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

// ========================================
// 8. Keep file operations disabled
// ========================================

organizeButton.disabled = true;

organizeButton.addEventListener("click", function () {
    showNotification(
        "Organization is disabled until the safe backend is implemented."
    );
});
