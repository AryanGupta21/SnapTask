const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(express.json());

const SKILLS_DIR = path.join(__dirname, '..', 'openclaw-skills');
const OLLAMA_URL = 'http://localhost:11434/api/generate';
const MODEL = 'llama3.2';
const PORT = 3000;

// Load all skills at startup
function loadSkills() {
  const skills = {};
  for (const name of fs.readdirSync(SKILLS_DIR)) {
    const skillDir = path.join(SKILLS_DIR, name);
    if (!fs.statSync(skillDir).isDirectory()) continue;
    const mdPath = path.join(skillDir, 'SKILL.md');
    const indexPath = path.join(skillDir, 'index.js');
    if (!fs.existsSync(mdPath) || !fs.existsSync(indexPath)) continue;
    skills[name] = {
      description: fs.readFileSync(mdPath, 'utf8'),
      execute: require(indexPath).execute,
    };
  }
  return skills;
}

const skills = loadSkills();
console.log(`Loaded skills: ${Object.keys(skills).join(', ')}`);

function buildPrompt(rawText, entities) {
  const skillDocs = Object.entries(skills)
    .map(([name, s]) => `### Skill: ${name}\n${s.description}`)
    .join('\n\n');

  return `You are an intent classifier for the SnapTask app. Given OCR text from a phone camera image, identify what the user wants to do and extract parameters for the best matching skill.

Available skills:
${skillDocs}

OCR text:
"""
${rawText}
"""
${entities && entities.length > 0 ? `\nExtracted entities: ${JSON.stringify(entities)}` : ''}

Respond with ONLY valid JSON in this exact format (no markdown, no explanation):
{
  "intent": "<short description of what this is>",
  "summary": "<one sentence summary of the planned action>",
  "skill": "<skill name or null if no skill matches>",
  "params": { <skill parameters as key-value pairs> },
  "confidence": <0.0 to 1.0>
}

If no skill matches, set skill to null and params to {}.`;
}

async function callOllama(prompt) {
  const res = await fetch(OLLAMA_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: MODEL, prompt, stream: false }),
  });
  if (!res.ok) throw new Error(`Ollama error: ${res.status} ${await res.text()}`);
  const data = await res.json();
  return data.response;
}

function parseJson(text) {
  // Strip markdown code fences if Ollama wrapped the response
  const stripped = text.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '').trim();
  return JSON.parse(stripped);
}

app.post('/process', async (req, res) => {
  const { rawText, entities = [] } = req.body;
  if (!rawText || typeof rawText !== 'string') {
    return res.status(400).json({ error: 'rawText is required' });
  }

  console.log(`[POST /process] text length=${rawText.length}`);

  let llmResult;
  try {
    const prompt = buildPrompt(rawText, entities);
    const raw = await callOllama(prompt);
    console.log('[Ollama raw]', raw);
    llmResult = parseJson(raw);
  } catch (err) {
    console.error('[Ollama error]', err.message);
    return res.status(502).json({ error: `LLM error: ${err.message}` });
  }

  const { intent, summary, skill: skillName, params, confidence } = llmResult;

  let actions = [];
  if (skillName && skills[skillName]) {
    try {
      const action = await skills[skillName].execute(params);
      actions = [action];
    } catch (err) {
      console.error(`[Skill error: ${skillName}]`, err.message);
      // Return the response without actions rather than failing entirely
    }
  }

  const response = {
    intent: intent ?? 'unknown',
    summary: summary ?? rawText.slice(0, 100),
    confidence: confidence ?? 0,
    actions,
  };

  console.log('[Response]', JSON.stringify(response));
  res.json(response);
});

app.get('/health', (_req, res) => res.json({ status: 'ok', skills: Object.keys(skills) }));

app.listen(PORT, () => {
  console.log(`OpenClaw Gateway listening on http://localhost:${PORT}`);
  console.log(`Using Ollama model: ${MODEL}`);
});
