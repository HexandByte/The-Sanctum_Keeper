
"use strict";

const { spawn } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

class JavaBridge {
    constructor({ jarPath, configDirectory }) {
        this.jarPath = jarPath;
        this.configDirectory = configDirectory;

        this.child = null;
        this.ready = false;
        this.startPromise = null;

        this.buffer = "";
        this.nextId = 0;
        this.pending = new Map();

        this.resolveReady = null;
        this.rejectReady = null;
        this.startupTimer = null;

        // Allow only commands implemented by the Java backend.
        this.allowedCommands = new Set([
            "getRules",
            "saveRules",
            "preview",
            "organize",
            "undo"
        ]);
    }

    // Start Java once and wait if startup is already in progress.
    async start() {
        if (this.child && this.ready) {
            return;
        }

        if (this.startPromise) {
            return this.startPromise;
        }

        this.startPromise = this.launch();

        try {
            await this.startPromise;
        } finally {
            this.startPromise = null;
        }
    }

    // Launch the JAR and wait for Java's ready message.
    launch() {
        return new Promise((resolve, reject) => {
            if (!fs.existsSync(this.jarPath)) {
                reject(
                    new Error(
                        "Java engine JAR was not found: " + this.jarPath
                    )
                );
                return;
            }

            // Create the directory where Java stores its rules.
            try {
                fs.mkdirSync(this.configDirectory, {
                    recursive: true
                });
            } catch (error) {
                reject(
                    new Error(
                        "Could not create the engine configuration directory: " +
                        error.message
                    )
                );
                return;
            }

            // Prefer the Java installation specified by JAVA_HOME.
            const javaExecutable = process.env.JAVA_HOME
                ? path.join(
                    process.env.JAVA_HOME,
                    "bin",
                    process.platform === "win32" ? "java.exe" : "java"
                )
                : "java";

            const child = spawn(
                javaExecutable,
                [
                    "-jar",
                    this.jarPath,
                    this.configDirectory
                ],
                {
                    windowsHide: true,
                    stdio: ["pipe", "pipe", "pipe"]
                }
            );

            this.child = child;
            this.ready = false;
            this.buffer = "";

            this.resolveReady = resolve;
            this.rejectReady = reject;

            // Stop startup if Java does not become ready in 15 seconds.
            this.startupTimer = setTimeout(() => {
                if (!this.ready) {
                    this.rejectReady?.(
                        new Error("Java engine startup timed out.")
                    );

                    child.kill();
                }
            }, 15000);

            // Java's stdout carries the JSON protocol.
            child.stdout.setEncoding("utf8");

            child.stdout.on("data", (chunk) => {
                this.handleStdout(chunk);
            });

            // Java diagnostics belong on stderr, not stdout.
            child.stderr.setEncoding("utf8");

            child.stderr.on("data", (chunk) => {
                console.error("[Java engine]", chunk.trimEnd());
            });

            child.on("error", (error) => {
                const failure = new Error(
                    "Could not start the Java engine: " + error.message
                );

                this.clearStartupTimer();
                this.rejectReady?.(failure);
                this.failPending(failure);

                this.ready = false;
                this.child = null;
            });

            child.on("exit", (code, signal) => {
                const failure = new Error(
                    `Java engine exited (code: ${code}, signal: ${signal ?? "none"}).`
                );

                this.clearStartupTimer();
                this.rejectReady?.(failure);
                this.failPending(failure);

                this.ready = false;
                this.child = null;

                console.error("[Java engine]", failure.message);
            });
        });
    }

    // Buffer incoming text because one message may arrive in multiple chunks.
    handleStdout(chunk) {
        this.buffer += chunk;

        // Each complete line contains one JSON message.
        while (this.buffer.includes("\n")) {
            const newlineIndex = this.buffer.indexOf("\n");

            const line = this.buffer
                .slice(0, newlineIndex)
                .trim();

            this.buffer = this.buffer.slice(newlineIndex + 1);

            if (!line) {
                continue;
            }

            let message;

            try {
                message = JSON.parse(line);
            } catch (error) {
                console.error(
                    "[Java engine] Invalid JSON on stdout:",
                    line
                );
                continue;
            }

            // Handle Java's startup handshake separately from normal responses.
            if (message.type === "ready") {
                if (message.protocolVersion !== 1) {
                    const failure = new Error(
                        "Unsupported Java engine protocol version."
                    );

                    this.clearStartupTimer();
                    this.rejectReady?.(failure);
                    this.child?.kill();

                    continue;
                }

                this.ready = true;
                this.clearStartupTimer();

                const resolve = this.resolveReady;
                this.resolveReady = null;
                this.rejectReady = null;

                resolve?.();
                continue;
            }

            if (message.id === undefined || message.id === null) {
                console.error(
                    "[Java engine] Response has no request ID:",
                    message
                );
                continue;
            }

            // Match the response to the request that generated it.
            const id = String(message.id);
            const request = this.pending.get(id);

            if (!request) {
                console.error(
                    "[Java engine] Response has no pending request:",
                    id
                );
                continue;
            }

            clearTimeout(request.timer);
            this.pending.delete(id);

            if (message.ok === true) {
                request.resolve(message.data);
            } else {
                const error = new Error(
                    message.error?.message || "Java engine request failed."
                );

                error.code = message.error?.code || "ENGINE_ERROR";
                request.reject(error);
            }
        }
    }

    // Validate a command, send it to Java, and wait for the response.
    async send(command, payload = {}) {
        if (!this.allowedCommands.has(command)) {
            throw new Error("Unsupported engine command.");
        }

        const requestFields = {};

        if (command === "saveRules") {
            if (!Array.isArray(payload.rules)) {
                throw new Error("Rules must be an array.");
            }

            requestFields.rules = payload.rules;
        }

        if (command === "preview" || command === "organize") {
            if (
                typeof payload.sourceFolder !== "string" ||
                !payload.sourceFolder.trim()
            ) {
                throw new Error("A source folder must be selected.");
            }

            requestFields.sourceFolder = payload.sourceFolder;
        }

        await this.start();

        if (!this.child || !this.ready) {
            throw new Error("Java engine is not ready.");
        }

        const id = "req-" + (++this.nextId);

        // Build the request using the field names expected by Main.java.
        const message = {
            id,
            command,
            ...requestFields
        };

        return new Promise((resolve, reject) => {
            // Prevent a request from waiting indefinitely.
            const timer = setTimeout(() => {
                this.pending.delete(id);

                reject(
                    new Error("Java engine request timed out.")
                );
            }, 120000);

            this.pending.set(id, {
                resolve,
                reject,
                timer
            });

            try {
                // Java reads one JSON request per line.
                this.child.stdin.write(
                    JSON.stringify(message) + "\n",
                    "utf8",
                    (error) => {
                        if (!error) {
                            return;
                        }

                        const request = this.pending.get(id);

                        if (request) {
                            clearTimeout(request.timer);
                            this.pending.delete(id);
                            request.reject(error);
                        }
                    }
                );
            } catch (error) {
                clearTimeout(timer);
                this.pending.delete(id);
                reject(error);
            }
        });
    }

    // Cancel the startup timeout when it is no longer needed.
    clearStartupTimer() {
        if (this.startupTimer) {
            clearTimeout(this.startupTimer);
            this.startupTimer = null;
        }
    }

    // Reject every request still waiting for Java.
    failPending(error) {
        for (const request of this.pending.values()) {
            clearTimeout(request.timer);
            request.reject(error);
        }

        this.pending.clear();
    }

    // Stop Java and notify callers waiting for responses.
    stop() {
        this.clearStartupTimer();

        if (this.child && !this.child.killed) {
            this.child.kill();
        }

        this.ready = false;

        this.failPending(
            new Error("Java engine is shutting down.")
        );
    }
}

module.exports = JavaBridge;
