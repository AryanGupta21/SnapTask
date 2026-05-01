require('dotenv').config();
const express = require('express');
const fs = require('fs');
const path = require('path');
const { GoogleGenAI } = require('@google/genai');

const app = express();
app.use(express.json());

const SKILLS_DIR = path.join(__dirname, '..', 'openclaw-skills');
const PORT = 3000;

if (!process.env.GEMINI_API_KEY) {
  console.error('ERROR: GEMINI_API_KEY is not set. Create a .env file — see .env.example.');
  process.exit(1);
}

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

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

Respond with valid JSON in this exact format:
{
  "intent": "<short description of what this is>",
  "summary": "<one sentence summary of the planned action>",
  "skill": "<skill name or null if no skill matches>",
  "params": {},
  "confidence": 0.0
}

If no skill matches, set skill to null and params to {}.`;
}

app.post('/process', async (req, res) => {
  const { rawText, entities = [] } = req.body;
  if (!rawText || typeof rawText !== 'string') {
    return res.status(400).json({ error: 'rawText is required' });
  }

  console.log(`[POST /process] text length=${rawText.length}`);

  let llmResult;
  try {
    const result = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: buildPrompt(rawText, entities),
      config: { responseMimeType: 'application/json' },
    });
    console.log('[Gemini raw]', result.text);
    llmResult = JSON.parse(result.text);
  } catch (err) {
    console.error('[Gemini error]', err.message);
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

app.get('/health', (_req, res) => res.json({ status: 'ok', skills: Object.keys(skills), model: 'gemini-2.0-flash' }));

app.listen(PORT, () => {
  console.log(`OpenClaw Gateway listening on http://localhost:${PORT}`);
  console.log('Using Gemini 2.0 Flash');
});
