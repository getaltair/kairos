import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

interface SkillTriggers {
	keywords?: string[];
	intentPatterns?: string[];
}

interface SkillConfig {
	promptTriggers?: SkillTriggers;
	priority?: "critical" | "high" | "medium" | "low";
	/**
	 * Stack keys this skill/agent is scoped to. If absent, the skill is
	 * treated as universal (available regardless of project stack).
	 *
	 * Stack keys match entries in `Context/stack.md`'s `## Detected stacks`
	 * section (which in turn matches keys in `stacks.json`).
	 */
	stacks?: string[];
}

interface RulesFile {
	skills?: Record<string, SkillConfig>;
	agents?: Record<string, SkillConfig>;
}

interface SessionState {
	skills: string[];
	agents: string[];
	lastUpdated: string;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Resolve the project root from the hook's install location.
 * Assumes the hook lives at `<project>/.claude/hooks/skill-activation/`.
 */
function resolveProjectRoot(): string {
	// __dirname is .claude/hooks/skill-activation -> up three is project root
	return join(__dirname, "..", "..", "..");
}

/**
 * Read `Context/stack.md` and extract the detected stack keys.
 *
 * Format expected (per /setup output):
 * ```
 * ## Detected stacks
 *
 * - `python-fastapi` — Python / FastAPI (in ./src/)
 * - `vue-typescript` — Vue.js / TypeScript (in ./frontend/)
 * ```
 *
 * Returns an empty array if the file is missing or the section is empty —
 * callers should treat that as "no stacks detected" and only activate
 * universal skills (those without a `stacks` field).
 */
function readDetectedStacks(projectRoot: string): string[] {
	const stackFile = join(projectRoot, "Context", "stack.md");
	if (!existsSync(stackFile)) return [];

	let contents: string;
	try {
		contents = readFileSync(stackFile, "utf-8");
	} catch {
		return [];
	}

	// Find the `## Detected stacks` section and parse bullets until the
	// next `##` heading or EOF.
	const sectionRe = /^##\s+Detected stacks\s*$/m;
	const match = sectionRe.exec(contents);
	if (!match) return [];

	const afterHeading = contents.slice(match.index + match[0].length);
	const nextHeading = /^##\s/m.exec(afterHeading);
	const section = nextHeading
		? afterHeading.slice(0, nextHeading.index)
		: afterHeading;

	// Bullet format: `- \`<stack-key>\` — ...`
	const bulletRe = /^\s*-\s+`([a-z0-9][a-z0-9-]*)`/gim;
	const keys: string[] = [];
	let m: RegExpExecArray | null;
	while ((m = bulletRe.exec(section)) !== null) {
		keys.push(m[1]);
	}
	return keys;
}

/**
 * Determine whether a skill/agent is applicable given the detected stacks.
 *
 * - No `stacks` field  → universal, always applicable.
 * - `stacks: []`       → universal (empty list treated same as absent).
 * - `stacks: [...]`    → applicable if at least one entry is in detectedStacks.
 */
function isStackApplicable(
	config: SkillConfig,
	detectedStacks: string[],
): boolean {
	if (!config.stacks || config.stacks.length === 0) return true;
	return config.stacks.some((s) => detectedStacks.includes(s));
}

function main(): void {
	const input = readFileSync(0, "utf-8");
	const data = JSON.parse(input) as { prompt: string; session_id: string };
	const prompt = data.prompt.toLowerCase();
	const sessionId = data.session_id;

	// Load rules
	const skillRulesPath = join(__dirname, "skill-rules.json");
	const agentRulesPath = join(__dirname, "agent-rules.json");

	const skillRules: RulesFile = existsSync(skillRulesPath)
		? JSON.parse(readFileSync(skillRulesPath, "utf-8"))
		: { skills: {} };

	const agentRules: RulesFile = existsSync(agentRulesPath)
		? JSON.parse(readFileSync(agentRulesPath, "utf-8"))
		: { agents: {} };

	// Resolve stacks for stack-scoped filtering. A missing or empty
	// Context/stack.md means no stack-scoped skills/agents will match —
	// only universal ones (no `stacks` field) remain available.
	const projectRoot = resolveProjectRoot();
	const detectedStacks = readDetectedStacks(projectRoot);

	// State tracking per session
	const stateDir = join(__dirname, ".state");
	mkdirSync(stateDir, { recursive: true });
	const stateFilePath = join(stateDir, "recommendations.json");

	let state: Record<string, SessionState> = {};
	if (existsSync(stateFilePath)) {
		try {
			state = JSON.parse(readFileSync(stateFilePath, "utf-8"));
		} catch {
			state = {};
		}
	}

	// Cleanup sessions older than 7 days
	const cutoff = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
	for (const [sid, sessionData] of Object.entries(state)) {
		if (sessionData.lastUpdated < cutoff) delete state[sid];
	}

	const alreadySkills = state[sessionId]?.skills ?? [];
	const alreadyAgents = state[sessionId]?.agents ?? [];

	type Match = { name: string; config: SkillConfig };
	const matchedSkills: Match[] = [];
	const matchedAgents: Match[] = [];

	// Match skills
	for (const [name, config] of Object.entries(skillRules.skills ?? {})) {
		const triggers = config.promptTriggers;
		if (!triggers || alreadySkills.includes(name)) continue;
		if (!isStackApplicable(config, detectedStacks)) continue;

		const kwMatch = triggers.keywords?.some((kw) =>
			prompt.includes(kw.toLowerCase()),
		);
		if (kwMatch) {
			matchedSkills.push({ name, config });
			continue;
		}

		const intentMatch = triggers.intentPatterns?.some((p) =>
			new RegExp(p, "i").test(prompt),
		);
		if (intentMatch) matchedSkills.push({ name, config });
	}

	// Match agents
	for (const [name, config] of Object.entries(agentRules.agents ?? {})) {
		const triggers = config.promptTriggers;
		if (!triggers || alreadyAgents.includes(name)) continue;
		if (!isStackApplicable(config, detectedStacks)) continue;

		const kwMatch = triggers.keywords?.some((kw) =>
			prompt.includes(kw.toLowerCase()),
		);
		if (kwMatch) {
			matchedAgents.push({ name, config });
			continue;
		}

		const intentMatch = triggers.intentPatterns?.some((p) =>
			new RegExp(p, "i").test(prompt),
		);
		if (intentMatch) matchedAgents.push({ name, config });
	}

	// Generate output
	if (matchedSkills.length > 0 || matchedAgents.length > 0) {
		const lines: string[] = [];
		const groupByPriority = (items: Match[]) => ({
			critical: items.filter((s) => s.config.priority === "critical"),
			high: items.filter((s) => s.config.priority === "high"),
			medium: items.filter((s) => s.config.priority === "medium"),
			low: items.filter((s) => s.config.priority === "low"),
		});

		if (matchedSkills.length > 0) {
			lines.push("SKILL ACTIVATION CHECK\n");
			const g = groupByPriority(matchedSkills);
			if (g.critical.length) {
				lines.push("CRITICAL SKILLS (REQUIRED):");
				g.critical.forEach((s) => {
					lines.push(`  -> ${s.name}`);
				});
				lines.push("");
			}
			if (g.high.length) {
				lines.push("RECOMMENDED SKILLS:");
				g.high.forEach((s) => {
					lines.push(`  -> ${s.name}`);
				});
				lines.push("");
			}
			if (g.medium.length) {
				lines.push("SUGGESTED SKILLS:");
				g.medium.forEach((s) => {
					lines.push(`  -> ${s.name}`);
				});
				lines.push("");
			}
			if (g.low.length) {
				lines.push("OPTIONAL SKILLS:");
				g.low.forEach((s) => {
					lines.push(`  -> ${s.name}`);
				});
				lines.push("");
			}
			lines.push("ACTION: Use Skill tool BEFORE responding\n");
		}

		if (matchedAgents.length > 0) {
			lines.push("AGENT ACTIVATION CHECK\n");
			const g = groupByPriority(matchedAgents);
			if (g.critical.length) {
				lines.push("CRITICAL AGENTS (REQUIRED):");
				g.critical.forEach((a) => {
					lines.push(`  -> ${a.name}`);
				});
				lines.push("");
			}
			if (g.high.length) {
				lines.push("RECOMMENDED AGENTS:");
				g.high.forEach((a) => {
					lines.push(`  -> ${a.name}`);
				});
				lines.push("");
			}
			if (g.medium.length) {
				lines.push("SUGGESTED AGENTS:");
				g.medium.forEach((a) => {
					lines.push(`  -> ${a.name}`);
				});
				lines.push("");
			}
			if (g.low.length) {
				lines.push("OPTIONAL AGENTS:");
				g.low.forEach((a) => {
					lines.push(`  -> ${a.name}`);
				});
				lines.push("");
			}
			lines.push("ACTION: Use Task tool with appropriate subagent_type\n");
		}

		console.log(lines.join("\n"));

		// Update state
		state[sessionId] = {
			skills: [...alreadySkills, ...matchedSkills.map((s) => s.name)],
			agents: [...alreadyAgents, ...matchedAgents.map((a) => a.name)],
			lastUpdated: new Date().toISOString(),
		};
		writeFileSync(stateFilePath, JSON.stringify(state, null, 2), "utf-8");
	}

	process.exit(0);
}

try {
	main();
} catch (err) {
	console.error("Skill activation error:", err);
	process.exit(1);
}
