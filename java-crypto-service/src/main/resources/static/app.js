const output = document.getElementById("output");
const downloadBtn = document.getElementById("downloadBtn");
const textInput = document.getElementById("textInput");
const fileInput = document.getElementById("fileInput");
const signatureInput = document.getElementById("signatureInput");
const urlInput = document.getElementById("urlInput");
const detachedVerify = document.getElementById("detachedVerify");
const textField = document.getElementById("textField");
const fileField = document.getElementById("fileField");
const sourceToggle = document.getElementById("sourceToggle");

let lastResult = null;
let source = "text";

function selectSource(value) {
    source = value;
    sourceToggle.querySelectorAll("button").forEach((b) => b.classList.toggle("active", b.dataset.source === value));
    textField.hidden = value !== "text";
    fileField.hidden = value !== "file";
}

sourceToggle.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => selectSource(b.dataset.source)));

function bytesToBase64(bytes) {
    let binary = "";
    bytes.forEach((b) => (binary += String.fromCharCode(b)));
    return btoa(binary);
}

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

function decodeText(base64) {
    try {
        return new TextDecoder("utf-8", { fatal: true }).decode(base64ToBytes(base64));
    } catch (error) {
        return null;
    }
}

async function inputAsBase64() {
    if (source === "file") {
        if (!fileInput.files.length) {
            throw new Error("Файл не выбран");
        }
        const buffer = await fileInput.files[0].arrayBuffer();
        return bytesToBase64(new Uint8Array(buffer));
    }
    return bytesToBase64(new TextEncoder().encode(textInput.value));
}

function cmsBlobBase64() {
    const pasted = signatureInput.value.trim();
    if (!pasted) {
        throw new Error("Вставьте подпись или конверт (base64) в поле «Подпись / конверт»");
    }
    return pasted;
}

function show(result, isError) {
    let text = typeof result === "string" ? result : JSON.stringify(result, null, 2);
    if (!isError && result && typeof result === "object" && typeof result.data === "string") {
        const decoded = decodeText(result.data);
        text += decoded !== null
            ? "\n\nРасшифрованный текст:\n" + decoded
            : "\n\n(бинарные данные — воспользуйтесь «Скачать результат»)";
    }
    output.textContent = text;
    output.className = isError ? "error" : "ok";
    lastResult = isError ? null : result;
    downloadBtn.disabled = isError;
}

function fileNameFromUrl(rawUrl) {
    try {
        const segment = new URL(rawUrl).pathname.split("/").filter(Boolean).pop();
        return segment && segment.includes(".") ? decodeURIComponent(segment) : "";
    } catch (error) {
        return "";
    }
}

function download() {
    if (!lastResult) return;
    let blob;
    let name;
    if (typeof lastResult.data === "string") {
        blob = new Blob([base64ToBytes(lastResult.data)], { type: "application/octet-stream" });
        name = "decrypted.bin";
    } else if (typeof lastResult.document === "string") {
        blob = new Blob([base64ToBytes(lastResult.document)], { type: lastResult.contentType || "application/octet-stream" });
        name = fileNameFromUrl(urlInput.value) || "document.bin";
    } else {
        const payload = lastResult.signature || lastResult.envelope || lastResult.hash || "";
        blob = new Blob([payload], { type: "text/plain" });
        name = "crypto-result.txt";
    }
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = name;
    link.click();
    URL.revokeObjectURL(link.href);
}

async function call(path, body) {
    const response = await fetch(path, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    const json = await response.json();
    if (!response.ok) {
        throw new Error(json.error || `HTTP ${response.status}`);
    }
    return json;
}

async function verifySignature() {
    const signature = cmsBlobBase64();
    if (detachedVerify.checked) {
        return call("/api/v1/signatures/verify", { signature, data: await inputAsBase64(), detached: true });
    }
    return call("/api/v1/signatures/verify", { signature, detached: false });
}

const handlers = {
    "sign-attached": async () => call("/api/v1/signatures/sign", { data: await inputAsBase64(), detached: false }),
    "sign-detached": async () => call("/api/v1/signatures/sign", { data: await inputAsBase64(), detached: true }),
    "verify": verifySignature,
    "encrypt": async () => call("/api/v1/encryption/encrypt", { data: await inputAsBase64() }),
    "decrypt": async () => call("/api/v1/encryption/decrypt", { envelope: cmsBlobBase64() }),
    "hash": async () => call("/api/v1/hash", { data: await inputAsBase64(), algorithm: "SHA-256" }),
    "fetch": async () => call("/api/v1/documents/fetch", { url: urlInput.value.trim() }),
};

document.querySelectorAll("button[data-op]").forEach((button) => {
    button.addEventListener("click", async () => {
        try {
            show(await handlers[button.dataset.op](), false);
        } catch (error) {
            show(error.message, true);
        }
    });
});

downloadBtn.addEventListener("click", download);
