#!/usr/bin/env bun
/**
 * Cortex Backup Compactor
 *
 * Summarizes old backup entries (>14 days) in `.cortex/backups/` into
 * consolidated summary files in `.cortex/backups/archived/`. Uses the
 * Claude CLI in print mode (`claude -p`) for text-only summarization,
 * which runs against the user's subscription rather than the API.
 *
 * Each summary file contains paragraph-length summaries per session
 * plus a Session Index table preserving session IDs for
 * `claude --resume <session-id>` access. Originals are deleted after
 * the summary is successfully written.
 *
 * Cortex notes (vs ClaudeFast v5.2):
 * - Backups live in `.cortex/backups/` (not `.claude/backups/`)
 * - Filename format is `{num}-backup-{Mon} {day}{ord} {h}:{m}{ampm}.md`
 *   with spaces and a colon, no year
 * - Year is inferred from the file mtime to handle the Dec->Jan boundary
 *
 * Usage: bun run .claude/hooks/context-recovery/backup-compactor.ts
 * Env: CLAUDE_INVOKED_BY=cortex_backup_compactor (set automatically)
 */

import {
	existsSync,
	mkdirSync,
	readFileSync,
	readdirSync,
	statSync,
	unlinkSync,
	writeFileSync,
} from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// Recursion guard: set BEFORE any Claude CLI invocation so any hooks
// triggered by `claude -p` see the env and skip themselves.
process.env.CLAUDE_INVOKED_BY = "cortex_backup_compactor";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// ============================================================================
// CONFIGURATION
// ============================================================================

const STALE_THRESHOLD_DAYS = 14;
const BATCH_SIZE = 7;
const MAX_BATCHES_PER_RUN = 5;
const MAX_CONTENT_PER_BACKUP = 4000; // chars per backup sent to Claude
const DELAY_BETWEEN_BATCHES_MS = 5000;

// Lock under ~/.claude so concurrent compactor runs across projects
// can't stomp on each other.
const COMPACTOR_LOCK_PATH = join(
	homedir(),
	".claude",
	"cortex-compactor.lock",
);

// ============================================================================
// PATHS AND LOGGING
// ============================================================================

function getProjectDir(): string {
	// .claude/hooks/context-recovery/ -> ../../../  (project root)
	return join(__dirname, "..", "..", "..");
}

function getBackupDir(): string {
	return join(getProjectDir(), ".cortex", "backups");
}

function getArchiveDir(): string {
	const dir = join(getBackupDir(), "archived");
	mkdirSync(dir, { recursive: true });
	return dir;
}

function getLogPath(): string {
	return join(getBackupDir(), ".compactor.log");
}

function log(msg: string): void {
	try {
		const dir = getBackupDir();
		mkdirSync(dir, { recursive: true });
		const line = `[${new Date().toISOString()}] ${msg}\n`;
		// Use appendFileSync via writeFileSync flag 'a'
		const path = getLogPath();
		writeFileSync(path, line, { flag: "a" });
	} catch {
		// last resort -- if we can't even log, still try stderr
		try {
			console.error(`[compactor] ${msg}`);
		} catch {
			/* give up */
		}
	}
}

// ============================================================================
// FILENAME PARSING
//
// Cortex backup format: `{num}-backup-{Mon} {day}{ord} {h}:{m}{ampm}.md`
// Example: `42-backup-Apr 18th 9:34pm.md`
// No year is encoded -- we infer it from file mtime.
// ============================================================================

const MONTHS: Record<string, number> = {
	Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
	Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11,
};

const FILENAME_RE =
	/^(\d+)-backup-(\w+) (\d+)(?:st|nd|rd|th) (\d+):(\d+)(am|pm)\.md$/;

function getBackupNumber(filename: string): number {
	const m = filename.match(/^(\d+)-/);
	return m ? parseInt(m[1], 10) : 0;
}

/**
 * Parse the date encoded in a Cortex backup filename. The year comes
 * from the file mtime since the filename does not include it. If the
 * mtime year produces a date in the future relative to mtime (e.g.
 * filename says Dec but mtime is Jan), step the year back by one.
 */
function parseDateFromFilename(
	filename: string,
	backupDir: string,
): Date | null {
	const m = filename.match(FILENAME_RE);
	if (!m) return null;

	const [, , monthStr, day, hour, min, ampm] = m;
	const month = MONTHS[monthStr];
	if (month === undefined) return null;

	let h = parseInt(hour, 10);
	if (ampm === "pm" && h !== 12) h += 12;
	if (ampm === "am" && h === 12) h = 0;

	let year: number;
	try {
		year = statSync(join(backupDir, filename)).mtime.getFullYear();
	} catch {
		year = new Date().getFullYear();
	}

	let dt = new Date(year, month, parseInt(day, 10), h, parseInt(min, 10));
	// Guard: if the parsed date is more than 30 days after mtime, the
	// filename is from the prior year (Dec file, Jan mtime).
	try {
		const mtime = statSync(join(backupDir, filename)).mtime;
		if (dt.getTime() - mtime.getTime() > 30 * 24 * 60 * 60 * 1000) {
			dt = new Date(year - 1, month, parseInt(day, 10), h, parseInt(min, 10));
		}
	} catch {
		/* keep dt */
	}
	return dt;
}

// ============================================================================
// CONTENT PROCESSING
// ============================================================================

function trimBackupContent(content: string): string {
	const lines = content.split("\n");
	const trimmed: string[] = [];
	let inClaudeResponses = false;
	let responseCount = 0;

	for (const line of lines) {
		if (line.startsWith("## Claude's Key Responses")) {
			inClaudeResponses = true;
			trimmed.push(line);
			continue;
		}
		if (line.startsWith("## ") && inClaudeResponses) {
			inClaudeResponses = false;
		}

		if (inClaudeResponses) {
			if (line.startsWith("- ")) {
				responseCount++;
				if (responseCount <= 3) {
					trimmed.push(
						line.slice(0, 200) + (line.length > 200 ? "..." : ""),
					);
				}
			}
			continue;
		}
		trimmed.push(line);
	}

	return trimmed.join("\n").slice(0, MAX_CONTENT_PER_BACKUP);
}

function extractSessionId(content: string): string {
	const m = content.match(/\*\*Session ID:\*\*\s*(\S+)/);
	return m ? m[1] : "unknown";
}

function extractSessionDates(content: string): {
	start: string | null;
	end: string | null;
} {
	const start = content.match(/\*\*Session Start:\*\*\s*(.+)/);
	const end = content.match(/\*\*Session End:\*\*\s*(.+)/);
	return {
		start: start ? start[1].trim() : null,
		end: end ? end[1].trim() : null,
	};
}

// ============================================================================
// SUMMARIZATION via `claude -p`
// ============================================================================

function buildPrompt(batchFiles: string[], batchContents: string[]): string {
	let prompt = `You are summarizing Cortex session backups for long-term archival. For each session below, write a substantive paragraph (4-6 sentences) that captures:
- What the user was working on and why
- Key actions taken, files changed, or decisions made
- The outcome or state when the session ended
- Any notable patterns, tools used, or agents dispatched

Format your response as a series of markdown sections. Each section MUST use this exact heading format:
## Backup #N -- Session: <full-session-id>

Where N is the backup number and the session ID is copied exactly from the data below.

Write flowing paragraphs under each heading. Be specific and concrete, not vague. Do not use bullet points.

Here are the ${batchFiles.length} session backups to summarize:\n\n`;

	for (let i = 0; i < batchFiles.length; i++) {
		const num = getBackupNumber(batchFiles[i]);
		const sessionId = extractSessionId(batchContents[i]);
		const dates = extractSessionDates(batchContents[i]);
		const trimmed = trimBackupContent(batchContents[i]);

		prompt += `=== Backup #${num} | Session: ${sessionId} | Start: ${dates.start || "unknown"} | End: ${dates.end || "unknown"} ===\n`;
		prompt += trimmed;
		prompt += "\n\n";
	}
	return prompt;
}

function callClaude(prompt: string): string | null {
	log(`Calling Claude CLI for summarization (prompt length: ${prompt.length} chars)`);
	const result = spawnSync("claude", ["-p", "--model", "claude-sonnet-4-6"], {
		input: prompt,
		encoding: "utf-8",
		timeout: 180000, // 3 minutes
		env: { ...process.env, CLAUDE_INVOKED_BY: "cortex_backup_compactor" },
		cwd: getProjectDir(),
		windowsHide: true,
	});

	if (result.error) {
		log(`CLI error: ${result.error.message}`);
		return null;
	}
	if (result.status !== 0) {
		log(`CLI exit ${result.status}: ${(result.stderr || "").slice(0, 300)}`);
		return null;
	}
	return (result.stdout || "").trim() || null;
}

// ============================================================================
// OUTPUT FORMATTING
// ============================================================================

function formatSummaryFile(
	batchFiles: string[],
	batchContents: string[],
	summaryText: string,
	backupDir: string,
): string {
	const firstNum = getBackupNumber(batchFiles[0]);
	const lastNum = getBackupNumber(batchFiles[batchFiles.length - 1]);
	// Date range uses chronological min/max, not file-order first/last.
	// Files are sorted by backup number (creation order), so the newest
	// stale file is usually [0] and the oldest is at the end -- printing
	// them in file order would produce a backwards range like "Mar 18 - Mar 12".
	const allDates = batchFiles
		.map((f) => parseDateFromFilename(f, backupDir))
		.filter((d): d is Date => d !== null)
		.sort((a, b) => a.getTime() - b.getTime());
	const firstDate = allDates[0] ?? null;
	const lastDate = allDates[allDates.length - 1] ?? null;

	const dateRange =
		firstDate && lastDate
			? `${firstDate.toLocaleDateString("en-US", { month: "short", day: "numeric" })} - ${lastDate.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}`
			: "unknown range";

	// Build session index table -- preserves session IDs so a user can
	// always run `claude --resume <session-id>` even after the original
	// backup file is deleted.
	let sessionTable = "| # | Session ID | Date | Resume Command |\n";
	sessionTable += "|---|-----------|------|----------------|\n";
	for (let i = 0; i < batchFiles.length; i++) {
		const num = getBackupNumber(batchFiles[i]);
		const sessionId = extractSessionId(batchContents[i]);
		const date = parseDateFromFilename(batchFiles[i], backupDir);
		const dateStr = date
			? date.toLocaleDateString("en-US", {
					month: "short",
					day: "numeric",
					year: "numeric",
				})
			: "unknown";
		sessionTable += `| ${num} | ${sessionId} | ${dateStr} | \`claude --resume ${sessionId}\` |\n`;
	}

	let output = `# Session Summary (Backups #${firstNum}-${lastNum})\n\n`;
	output += `**Date Range:** ${dateRange}\n`;
	output += `**Sessions Compacted:** ${batchFiles.length}\n`;
	output += `**Generated:** ${new Date().toISOString()}\n\n`;
	output += `## Session Index\n\n`;
	output += sessionTable;
	output += `\n---\n\n`;
	output += summaryText;
	output += "\n";
	return output;
}

// ============================================================================
// LOCK MANAGEMENT
// ============================================================================

function acquireLock(): boolean {
	try {
		mkdirSync(dirname(COMPACTOR_LOCK_PATH), { recursive: true });
		if (existsSync(COMPACTOR_LOCK_PATH)) {
			const raw = readFileSync(COMPACTOR_LOCK_PATH, "utf-8");
			const lockTime = new Date(raw).getTime();
			const lockAge = Date.now() - lockTime;
			if (Number.isFinite(lockAge) && lockAge < 600000) {
				log(`Compactor already running (lock ${Math.round(lockAge / 1000)}s old), exiting`);
				return false;
			}
		}
		writeFileSync(COMPACTOR_LOCK_PATH, new Date().toISOString());
		return true;
	} catch (err) {
		log(`Lock acquisition failed: ${(err as Error).message}`);
		return false;
	}
}

function releaseLock(): void {
	try {
		if (existsSync(COMPACTOR_LOCK_PATH)) unlinkSync(COMPACTOR_LOCK_PATH);
	} catch {
		// best effort
	}
}

function deleteOriginals(backupDir: string, files: string[]): void {
	for (const f of files) {
		try {
			unlinkSync(join(backupDir, f));
		} catch (err) {
			log(`Failed to delete ${f}: ${(err as Error).message}`);
		}
	}
}

// ============================================================================
// MAIN
// ============================================================================

async function main(): Promise<void> {
	log("Backup compactor started");

	if (!acquireLock()) return;

	try {
		const backupDir = getBackupDir();
		if (!existsSync(backupDir)) {
			log("No backup directory found");
			return;
		}

		// Get all individual backup files (skip summaries and non-backup files)
		const allFiles = readdirSync(backupDir)
			.filter((f) => f.endsWith(".md") && /^\d+-backup-/.test(f))
			.sort((a, b) => getBackupNumber(a) - getBackupNumber(b));

		// Filter to files older than threshold
		const now = new Date();
		const threshold = new Date(
			now.getTime() - STALE_THRESHOLD_DAYS * 24 * 60 * 60 * 1000,
		);
		const staleFiles = allFiles.filter((f) => {
			const date = parseDateFromFilename(f, backupDir);
			return date && date < threshold;
		});

		if (staleFiles.length < BATCH_SIZE) {
			log(`Only ${staleFiles.length} stale backups (need ${BATCH_SIZE} for a full batch), skipping`);
			return;
		}
		log(`Found ${staleFiles.length} stale backups (older than ${STALE_THRESHOLD_DAYS} days)`);

		// Group into full batches only (drop partial remainder until next run)
		const batches: string[][] = [];
		for (let i = 0; i + BATCH_SIZE <= staleFiles.length; i += BATCH_SIZE) {
			batches.push(staleFiles.slice(i, i + BATCH_SIZE));
		}
		const batchesToProcess = batches.slice(0, MAX_BATCHES_PER_RUN);
		log(`Processing ${batchesToProcess.length} of ${batches.length} batches this run`);

		let processedCount = 0;
		for (let b = 0; b < batchesToProcess.length; b++) {
			const batch = batchesToProcess[b];
			log(`Batch ${b + 1}/${batchesToProcess.length}: ${batch.length} files (#${getBackupNumber(batch[0])}-${getBackupNumber(batch[batch.length - 1])})`);

			const contents = batch.map((f) => {
				try {
					return readFileSync(join(backupDir, f), "utf-8");
				} catch {
					return "";
				}
			});

			const prompt = buildPrompt(batch, contents);
			const summary = callClaude(prompt);
			if (!summary) {
				log(`Batch ${b + 1} summarization failed, skipping`);
				continue;
			}

			const archiveDir = getArchiveDir();
			const firstNum = getBackupNumber(batch[0]);
			const lastNum = getBackupNumber(batch[batch.length - 1]);
			const summaryContent = formatSummaryFile(batch, contents, summary, backupDir);
			const summaryFilename = `summary-${firstNum}-to-${lastNum}.md`;
			writeFileSync(join(archiveDir, summaryFilename), summaryContent);
			log(`Summary written: archived/${summaryFilename}`);

			// Originals deleted after summary is on disk -- session IDs
			// preserved in the table so `claude --resume` still works.
			deleteOriginals(backupDir, batch);
			processedCount += batch.length;

			if (b < batchesToProcess.length - 1) {
				await new Promise((resolve) => setTimeout(resolve, DELAY_BETWEEN_BATCHES_MS));
			}
		}

		log(`Compaction complete: ${processedCount} backups -> ${batchesToProcess.length} summaries`);
		if (batches.length > MAX_BATCHES_PER_RUN) {
			log(`${batches.length - MAX_BATCHES_PER_RUN} batches remaining for next run`);
		}
	} finally {
		releaseLock();
	}
}

main().catch((err) => {
	log(`Compactor error: ${(err as Error).message}`);
	releaseLock();
	process.exit(0);
});
