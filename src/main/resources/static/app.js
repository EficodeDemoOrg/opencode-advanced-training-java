const state = {
    items: [],
    deletingId: null
};

const elements = {
    body: document.querySelector("#inventory-body"),
    table: document.querySelector("#table-container"),
    loading: document.querySelector("#loading"),
    empty: document.querySelector("#empty"),
    message: document.querySelector("#message"),
    itemCount: document.querySelector("#item-count"),
    unitCount: document.querySelector("#unit-count"),
    zoneCount: document.querySelector("#zone-count"),
    itemDialog: document.querySelector("#item-dialog"),
    itemForm: document.querySelector("#item-form"),
    dialogMode: document.querySelector("#dialog-mode"),
    dialogTitle: document.querySelector("#dialog-title"),
    formError: document.querySelector("#form-error"),
    saveButton: document.querySelector("#save-item"),
    deleteDialog: document.querySelector("#delete-dialog"),
    deleteName: document.querySelector("#delete-item-name"),
    deleteError: document.querySelector("#delete-error"),
    deleteButton: document.querySelector("#confirm-delete")
};

document.querySelector("#add-item").addEventListener("click", openCreateDialog);
document.querySelector("#refresh-items").addEventListener("click", loadItems);
document.querySelector("#close-dialog").addEventListener("click", closeItemDialog);
document.querySelector("#cancel-dialog").addEventListener("click", closeItemDialog);
document.querySelector("#cancel-delete").addEventListener("click", closeDeleteDialog);
elements.itemForm.addEventListener("submit", saveItem);
elements.deleteButton.addEventListener("click", deleteItem);

elements.body.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const item = state.items.find(candidate => candidate.id === Number(button.dataset.id));
    if (!item) {
        return;
    }

    if (button.dataset.action === "edit") {
        openEditDialog(item);
    } else if (button.dataset.action === "delete") {
        openDeleteDialog(item);
    }
});

async function loadItems() {
    setPageError("");
    elements.loading.hidden = false;
    elements.table.hidden = true;
    elements.empty.hidden = true;

    try {
        const response = await fetch("/api/items");
        state.items = await readResponse(response);
        renderItems();
    } catch (error) {
        state.items = [];
        renderSummary();
        setPageError(error.message);
    } finally {
        elements.loading.hidden = true;
    }
}

function renderItems() {
    elements.body.replaceChildren();
    renderSummary();

    if (state.items.length === 0) {
        elements.table.hidden = true;
        elements.empty.hidden = false;
        return;
    }

    elements.empty.hidden = true;
    elements.table.hidden = false;
    for (const item of state.items) {
        elements.body.append(createRow(item));
    }
}

function createRow(item) {
    const row = document.createElement("tr");
    row.append(
        cell("Part", item.partNumber, "part-number"),
        componentCell(item),
        badgeCell(item.category),
        cell("Location", item.storageLocation, "location"),
        cell("Stock", item.quantity.toLocaleString(), "quantity number-column"),
        cell("Reorder", item.reorderLevel.toLocaleString(), "quantity number-column"),
        actionCell(item)
    );
    return row;
}

function cell(label, value, className = "") {
    const tableCell = document.createElement("td");
    tableCell.dataset.label = label;
    tableCell.className = className;
    tableCell.textContent = value;
    return tableCell;
}

function componentCell(item) {
    const tableCell = document.createElement("td");
    tableCell.dataset.label = "Component";

    const name = document.createElement("span");
    name.className = "component-name";
    name.textContent = item.name;
    tableCell.append(name);

    if (item.description) {
        const description = document.createElement("span");
        description.className = "component-description";
        description.textContent = item.description;
        description.title = item.description;
        tableCell.append(description);
    }
    return tableCell;
}

function badgeCell(category) {
    const tableCell = document.createElement("td");
    tableCell.dataset.label = "Category";
    const badge = document.createElement("span");
    badge.className = "category-badge";
    badge.textContent = category;
    tableCell.append(badge);
    return tableCell;
}

function actionCell(item) {
    const tableCell = document.createElement("td");
    tableCell.className = "actions";

    const edit = document.createElement("button");
    edit.type = "button";
    edit.className = "table-action";
    edit.dataset.action = "edit";
    edit.dataset.id = item.id;
    edit.textContent = "Edit";
    edit.setAttribute("aria-label", `Edit ${item.name}`);

    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "table-action delete";
    remove.dataset.action = "delete";
    remove.dataset.id = item.id;
    remove.textContent = "Remove";
    remove.setAttribute("aria-label", `Remove ${item.name}`);

    tableCell.append(edit, remove);
    return tableCell;
}

function renderSummary() {
    const totalUnits = state.items.reduce((total, item) => total + item.quantity, 0);
    const zones = new Set(state.items.map(item => item.storageLocation.split("-")[0]));
    elements.itemCount.textContent = state.items.length.toLocaleString();
    elements.unitCount.textContent = totalUnits.toLocaleString();
    elements.zoneCount.textContent = zones.size.toLocaleString();
}

function openCreateDialog() {
    elements.itemForm.reset();
    document.querySelector("#item-id").value = "";
    elements.dialogMode.textContent = "New record";
    elements.dialogTitle.textContent = "Add component";
    elements.saveButton.textContent = "Save component";
    setFormError("");
    elements.itemDialog.showModal();
    document.querySelector("#part-number").focus();
}

function openEditDialog(item) {
    elements.itemForm.reset();
    document.querySelector("#item-id").value = item.id;
    document.querySelector("#part-number").value = item.partNumber;
    document.querySelector("#name").value = item.name;
    document.querySelector("#category").value = item.category;
    document.querySelector("#storage-location").value = item.storageLocation;
    document.querySelector("#quantity").value = item.quantity;
    document.querySelector("#reorder-level").value = item.reorderLevel;
    document.querySelector("#description").value = item.description;
    elements.dialogMode.textContent = item.partNumber;
    elements.dialogTitle.textContent = "Edit component";
    elements.saveButton.textContent = "Update component";
    setFormError("");
    elements.itemDialog.showModal();
    document.querySelector("#name").focus();
}

function closeItemDialog() {
    elements.itemDialog.close();
}

async function saveItem(event) {
    event.preventDefault();
    setFormError("");
    const formData = new FormData(elements.itemForm);
    const id = formData.get("id");
    formData.delete("id");
    elements.saveButton.disabled = true;

    try {
        const response = await fetch(id ? `/api/items/${id}` : "/api/items", {
            method: id ? "PUT" : "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
            body: new URLSearchParams(formData)
        });
        await readResponse(response);
        closeItemDialog();
        await loadItems();
    } catch (error) {
        setFormError(error.message);
    } finally {
        elements.saveButton.disabled = false;
    }
}

function openDeleteDialog(item) {
    state.deletingId = item.id;
    elements.deleteName.textContent = `${item.name} (${item.partNumber})`;
    setDeleteError("");
    elements.deleteDialog.showModal();
    elements.deleteButton.focus();
}

function closeDeleteDialog() {
    state.deletingId = null;
    elements.deleteDialog.close();
}

async function deleteItem() {
    if (state.deletingId === null) {
        return;
    }
    setDeleteError("");
    elements.deleteButton.disabled = true;

    try {
        const response = await fetch(`/api/items/${state.deletingId}`, {method: "DELETE"});
        if (!response.ok) {
            await readResponse(response);
        }
        closeDeleteDialog();
        await loadItems();
    } catch (error) {
        setDeleteError(error.message);
    } finally {
        elements.deleteButton.disabled = false;
    }
}

async function readResponse(response) {
    if (response.status === 204) {
        return null;
    }

    let body;
    try {
        body = await response.json();
    } catch (_error) {
        throw new Error("The server returned an unreadable response");
    }

    if (!response.ok) {
        throw new Error(body.error || `Request failed with status ${response.status}`);
    }
    return body;
}

function setPageError(message) {
    elements.message.textContent = message;
    elements.message.hidden = !message;
}

function setFormError(message) {
    elements.formError.textContent = message;
    elements.formError.hidden = !message;
}

function setDeleteError(message) {
    elements.deleteError.textContent = message;
    elements.deleteError.hidden = !message;
}

loadItems();
