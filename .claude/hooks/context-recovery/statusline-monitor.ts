/**
 * StatusLine hook: renders the Cortex status bar and triggers
 * context-recovery backups at 30/15/5% remaining thresholds.
 *
 * Schema reference: code.claude.com/docs/en/statusline
 * The documented context field is context_window.remaining_percentage;
 * free_until_compact is kept as a legacy fallback.
 */
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { basename, join } from "node:path";
import { readState, runBackup, writeState } from "./backup-core.ts";

interface StatusInput {
	session_id: string;
	transcript_path: string;
	cwd?: string;
	model?: { id?: string; display_name?: string };
	workspace?: {
		current_dir?: string;
		project_dir?: string;
		git_worktree?: string;
	};
	cost?: {
		total_cost_usd?: number;
		total_lines_added?: number;
		total_lines_removed?: number;
	};
	context_window?: {
		used_percentage?: number | null;
		remaining_percentage?: number | null;
		/** Total context window size, defaulted to 200_000 when absent. */
		context_window_size?: number | null;
		/**
		 * Detailed token counts for the current turn. When present, these
		 * give an accurate token-used number that matches `/context`. When
		 * absent (e.g. before the first API call of the session), the dual
		 * trigger falls back to the percentage-based system.
		 */
		current_usage?: {
			input_tokens?: number | null;
			cache_creation_input_tokens?: number | null;
			cache_read_input_tokens?: number | null;
			output_tokens?: number | null;
		} | null;
	};
	vim?: { mode?: string };
	output_style?: { name?: string };
	free_until_compact?: number;
}

const THRESHOLDS = [30, 15, 5];

// Token-based backup thresholds (the primary system on large context
// windows -- 30% remaining on a 1M window means 700k tokens already used,
// which is far too late for a first backup). The percentage thresholds
// above act as a safety net for the 200k case where 50k is more than 25%.
const TOKEN_FIRST_BACKUP = 50_000;
const TOKEN_UPDATE_INTERVAL = 10_000;

// ---------- ANSI helpers ----------
const ESC = "\x1b[";
const RESET = `${ESC}0m`;
const wrap = (code: string, s: string) => `${ESC}${code}m${s}${RESET}`;
const dim = (s: string) => wrap("2", s);
const bold = (s: string) => wrap("1", s);
const cyan = (s: string) => wrap("36", s);
const green = (s: string) => wrap("32", s);
const yellow = (s: string) => wrap("33", s);
const red = (s: string) => wrap("31", s);
const magenta = (s: string) => wrap("35", s);
const blue = (s: string) => wrap("34", s);

// ---------- Git helpers ----------
// execFileSync (no shell) with a fixed argv list prevents injection even
// though all args here are hardcoded. Timeout guards a hung git process.
function git(cwd: string, args: string[]): string | null {
	try {
		return execFileSync("git", args, {
			cwd,
			stdio: ["ignore", "pipe", "ignore"],
			encoding: "utf-8",
			timeout: 200,
		}).trim();
	} catch {
		return null;
	}
}

// ---------- Effort level ----------
// Effort is not in the statusline stdin payload, so we reconstruct it from:
// 1. ~/.claude/history.jsonl — scan backward for the last `/effort <level>`
//    invocation in the current session (captures `/effort max` overrides).
// 2. ~/.claude/settings.json — `effortLevel` as the persistent default.
// Returns null if neither source yields a value. Any I/O error is swallowed.
function readSessionEffort(sessionId: string): string | null {
	try {
		const path = join(homedir(), ".claude", "history.jsonl");
		if (!existsSync(path)) return null;
		const text = readFileSync(path, "utf-8");
		const lines = text.split("\n");
		for (let i = lines.length - 1; i >= 0; i--) {
			const line = lines[i];
			if (!line || !line.includes(sessionId)) continue;
			if (!line.includes("/effort")) continue;
			try {
				const entry = JSON.parse(line) as {
					sessionId?: string;
					display?: string;
				};
				if (entry.sessionId !== sessionId) continue;
				const match = entry.display?.match(/^\/effort\s+(\w+)/);
				if (match) return match[1].toLowerCase();
			} catch {
				/* malformed line, skip */
			}
		}
	} catch {
		/* history unreadable, fall through */
	}
	return null;
}

function readDefaultEffort(): string | null {
	try {
		const path = join(homedir(), ".claude", "settings.json");
		if (!existsSync(path)) return null;
		const parsed = JSON.parse(readFileSync(path, "utf-8")) as {
			effortLevel?: unknown;
		};
		if (typeof parsed.effortLevel === "string") return parsed.effortLevel;
	} catch {
		/* settings unreadable */
	}
	return null;
}

function resolveEffort(sessionId: string): string | null {
	return readSessionEffort(sessionId) ?? readDefaultEffort();
}

// ---------- Segment builders ----------
function segModel(data: StatusInput, effort: string | null): string | null {
	const name = data.model?.display_name;
	if (!name) return null;
	return effort ? dim(`${name} · ${effort}`) : dim(name);
}

function segDir(data: StatusInput): string | null {
	const dir = data.workspace?.current_dir ?? data.cwd;
	if (!dir) return null;
	const home = homedir();
	const display = dir.startsWith(home) ? `~${dir.slice(home.length)}` : dir;
	return cyan(basename(display) || display);
}

function segWorktree(data: StatusInput): string | null {
	const name = data.workspace?.git_worktree;
	if (!name) return null;
	return dim(`wt:${name}`);
}

function segGit(cwd: string | undefined): string | null {
	if (!cwd) return null;
	if (git(cwd, ["rev-parse", "--is-inside-work-tree"]) !== "true") return null;

	let branch = git(cwd, ["symbolic-ref", "--quiet", "--short", "HEAD"]);
	if (!branch) {
		const sha = git(cwd, ["rev-parse", "--short", "HEAD"]);
		branch = sha ? `(${sha})` : "(detached)";
	}

	const parts: string[] = [green(branch)];

	const porcelain = git(cwd, ["status", "--porcelain"]);
	if (porcelain) {
		const count = porcelain.split("\n").filter(Boolean).length;
		if (count > 0) parts.push(yellow(`*${count}`));
	}

	const leftRight = git(cwd, [
		"rev-list",
		"--left-right",
		"--count",
		"HEAD...@{u}",
	]);
	if (leftRight) {
		const [ahead, behind] = leftRight.split(/\s+/).map((n) => Number(n) || 0);
		if (ahead > 0) parts.push(blue(`↑${ahead}`));
		if (behind > 0) parts.push(blue(`↓${behind}`));
	}

	return parts.join(" ");
}

function segDevEnv(cwd: string | undefined): string | null {
	if (process.env.VIRTUAL_ENV) {
		return magenta(`venv(${basename(process.env.VIRTUAL_ENV)})`);
	}
	if (process.env.CONDA_DEFAULT_ENV) {
		return magenta(`conda(${process.env.CONDA_DEFAULT_ENV})`);
	}
	if (!cwd) return null;
	const lockfiles: Array<[string, string]> = [
		["bun.lock", "bun"],
		["bun.lockb", "bun"],
		["pnpm-lock.yaml", "pnpm"],
		["yarn.lock", "yarn"],
		["package-lock.json", "npm"],
		["Cargo.toml", "cargo"],
		["go.mod", "go"],
		["pyproject.toml", "py"],
		["requirements.txt", "py"],
	];
	for (const [file, label] of lockfiles) {
		if (existsSync(join(cwd, file))) return magenta(label);
	}
	return null;
}

function segOutputStyle(data: StatusInput): string | null {
	const name = data.output_style?.name;
	if (!name || name === "default") return null;
	return dim(name);
}

function segContext(remaining: number | undefined): string | null {
	if (remaining === undefined) return null;
	const pct = Math.round(remaining);
	const label = `ctx ${pct}%`;
	if (pct <= 5) return bold(red(label));
	if (pct <= 15) return red(label);
	if (pct <= 30) return yellow(label);
	return green(label);
}

function segCost(data: StatusInput): string | null {
	const usd = data.cost?.total_cost_usd;
	if (!usd || usd <= 0) return null;
	return dim(`$${usd.toFixed(2)}`);
}

function segDiff(data: StatusInput): string | null {
	const added = data.cost?.total_lines_added ?? 0;
	const removed = data.cost?.total_lines_removed ?? 0;
	if (added <= 0 && removed <= 0) return null;
	const parts: string[] = [];
	if (added > 0) parts.push(green(`+${added}`));
	if (removed > 0) parts.push(red(`-${removed}`));
	return parts.join(" ");
}

// ---------- Main ----------
function deriveRemaining(data: StatusInput): number | undefined {
	const cw = data.context_window;
	if (cw?.remaining_percentage != null) return cw.remaining_percentage;
	if (cw?.used_percentage != null) return 100 - cw.used_percentage;
	if (data.free_until_compact != null) return data.free_until_compact;
	return undefined;
}

/**
 * Sum the four token fields from current_usage, returning undefined when
 * the field is absent. Returning undefined (not 0) is important: a real
 * 0 would constantly retrigger the "first backup at 50k" logic on a
 * fresh session, while undefined means "no token data this turn, skip
 * the token-based check entirely and fall through to percentage".
 */
function deriveTokenTotal(data: StatusInput): number | undefined {
	const usage = data.context_window?.current_usage;
	if (!usage) return undefined;
	const sum =
		(usage.input_tokens ?? 0) +
		(usage.cache_creation_input_tokens ?? 0) +
		(usage.cache_read_input_tokens ?? 0) +
		(usage.output_tokens ?? 0);
	return sum > 0 ? sum : undefined;
}

/**
 * Dual-trigger backup logic. Two systems run simultaneously, whichever
 * fires first wins on a given turn:
 *
 *   1. Token-based (primary on large windows): first backup at 50k tokens
 *      used, then every 10k after (50k, 60k, 70k, ...). On 1M context
 *      windows this fires long before any of the percentage thresholds.
 *   2. Percentage-based (safety net): the existing 30/15/5% thresholds
 *      plus continuous below the lowest threshold. Still the only path
 *      when current_usage is absent (e.g. before the first API call).
 */
function maybeBackup(data: StatusInput): void {
	const remaining = deriveRemaining(data);
	const tokens = deriveTokenTotal(data);
	if (remaining === undefined && tokens === undefined) return;

	const state = readState();
	const lastThreshold = state.lastBackupThreshold as number | null | undefined;
	const lastBackupAtTokens = state.lastBackupAtTokens ?? 0;

	let trigger: { reason: string; threshold?: number; atTokens?: number } | null = null;

	// --- Token-based trigger (primary) ---
	if (tokens !== undefined && tokens >= TOKEN_FIRST_BACKUP) {
		if (lastBackupAtTokens < TOKEN_FIRST_BACKUP) {
			trigger = {
				reason: `tokens_${Math.round(tokens / 1000)}k_first`,
				atTokens: tokens,
			};
		} else if (tokens - lastBackupAtTokens >= TOKEN_UPDATE_INTERVAL) {
			trigger = {
				reason: `tokens_${Math.round(tokens / 1000)}k_update`,
				atTokens: tokens,
			};
		}
	}

	// --- Percentage-based trigger (safety net) ---
	if (trigger === null && remaining !== undefined) {
		for (const threshold of THRESHOLDS) {
			if (
				remaining <= threshold &&
				(lastThreshold === null ||
					lastThreshold === undefined ||
					lastThreshold > threshold)
			) {
				trigger = { reason: `crossed_${threshold}pct`, threshold };
				break;
			}
		}
	}

	if (trigger === null) return;

	const result = runBackup(
		data.session_id,
		trigger.reason,
		data.transcript_path,
		remaining,
	);
	if (result) {
		const updated = readState();
		if (trigger.threshold !== undefined) {
			updated.lastBackupThreshold = trigger.threshold;
		}
		if (trigger.atTokens !== undefined) {
			updated.lastBackupAtTokens = trigger.atTokens;
		}
		writeState(updated);
	}
}

function build(data: StatusInput): string {
	const cwd = data.workspace?.current_dir ?? data.cwd;
	const remaining = deriveRemaining(data);
	// Vim mode and the effort indicator already appear in Claude Code's
	// built-in footer, so we omit segVim here and merge effort into segModel.
	const effort = resolveEffort(data.session_id);

	// Segments are grouped left-to-right:
	//   1. Claude identity   — model + effort, output style
	//   2. Location          — dir, worktree, dev env
	//   3. Git               — branch, dirty, ahead/behind
	//   4. Session metrics   — context %, cost, diff
	const segments: Array<string | null> = [
		// Claude identity
		segModel(data, effort),
		segOutputStyle(data),
		// Location
		segDir(data),
		segWorktree(data),
		segDevEnv(cwd),
		// Git
		segGit(cwd),
		// Session metrics
		segContext(remaining),
		segCost(data),
		segDiff(data),
	];

	const sep = ` ${dim("│")} `;
	return segments.filter((s): s is string => s !== null && s !== "").join(sep);
}

try {
	const input = readFileSync(0, "utf-8");
	const data = JSON.parse(input) as StatusInput;

	try {
		maybeBackup(data);
	} catch (err) {
		console.error("StatusLine backup error:", err);
	}

	const line = build(data);
	console.log(line || "Cortex");
	process.exit(0);
} catch (err) {
	console.error("StatusLine error:", err);
	console.log("Cortex");
	process.exit(0);
}
