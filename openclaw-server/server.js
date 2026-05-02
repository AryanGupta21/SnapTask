require('dotenv').config();

const express = require('express');
const fs = require('fs');
const path = require('path');
const { GoogleGenerativeAI } = require('@google/generative-ai');

const app = express();
app.use(express.json({ limit: '64kb' }));

const SKILLS_DIR = path.join(__dirname, '..', 'openclaw-skills');
const PORT = Number(process.env.PORT || 3000);
const MODEL_NAME = process.env.GEMINI_MODEL || 'gemini-1.5-flash';
const hasGeminiKey = Boolean(process.env.GEMINI_API_KEY);
const genAI = hasGeminiKey ? new GoogleGenerativeAI(process.env.GEMINI_API_KEY) : null;
const model = genAI
  ? genAI.getGenerativeModel({
      model: MODEL_NAME,
      generationConfig: { responseMimeType: 'application/json' },
    })
  : null;

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

function buildPrompt(rawText, entities) {
  const skillDocs = Object.entries(skills)
    .map(([name, skill]) => `### Skill: ${name}\n${skill.description}`)
    .join('\n\n');

  return `You are an intent classifier for SnapTask.

SnapTask receives OCR text from an Android phone. The image never leaves the phone.
Choose the best skill and extract parameters.

Available skills:
${skillDocs}

OCR text:
"""
${rawText}
"""

Extracted entities:
${JSON.stringify(entities)}

Respond with valid JSON only:
{
  "intent": "event | contact | note | expense | unknown",
  "summary": "short user-facing summary",
  "skill": "samsung-calendar | samsung-contacts | samsung-notes | expense-logger | null",
  "params": {},
  "confidence": 0.0
}`;
}

function fallbackClassify(rawText) {
  const text = rawText.trim();
  const lower = text.toLowerCase();
  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0] || null;
  const phone = text.match(/(?:\+?\d[\d\s().-]{7,}\d)/)?.[0]?.trim() || null;
  const amount = text.match(/(?:rs\.?|inr|usd|\$)\s*([0-9]+(?:\.[0-9]{1,2})?)/i)?.[1]
    || text.match(/([0-9]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|usd)/i)?.[1]
    || null;
  const hasDate = /\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|\d{1,2}[/-]\d{1,2})/i.test(text);
  const hasTime = /\b\d{1,2}(?::\d{2})?\s*(?:am|pm)\b/i.test(text);

  if (amount) {
    return {
      intent: 'expense',
      summary: 'Log an expense from the extracted text',
      skill: 'expense-logger',
      params: {
        merchant: firstLine(text),
        amount,
        currency: /(?:rs\.?|inr)/i.test(text) ? 'INR' : 'USD',
      },
      confidence: 0.7,
    };
  }

  if (email || phone) {
    return {
      intent: 'contact',
      summary: 'Create a contact from the extracted text',
      skill: 'samsung-contacts',
      params: {
        name: firstLine(text),
        phone,
        email,
      },
      confidence: 0.72,
    };
  }

  if (hasDate || hasTime) {
    return {
      intent: 'event',
      summary: 'Create a calendar event from the extracted text',
      skill: 'samsung-calendar',
      params: {
        title: firstLine(text),
        date: new Date().toISOString().split('T')[0],
        time: '09:00',
      },
      confidence: 0.58,
    };
  }

  return {
    intent: 'note',
    summary: 'Create a note from the extracted text',
    skill: 'samsung-notes',
    params: {
      title: firstLine(text),
      body: text,
    },
    confidence: 0.65,
  };
}

function firstLine(text) {
  return text.split(/\r?\n/).map((line) => line.trim()).find(Boolean) || 'Untitled';
}

async function classifyIntent(rawText, entities) {
  if (!model) {
    return { ...fallbackClassify(rawText), classifier: 'fallback' };
  }

  try {
    const result = await model.generateContent(buildPrompt(rawText, entities));
    const parsed = JSON.parse(result.response.text());
    return { ...parsed, classifier: MODEL_NAME };
  } catch (err) {
    console.warn(`[Gemini unavailable] ${err.message}`);
    return { ...fallbackClassify(rawText), classifier: 'fallback' };
  }
}

async function buildResponse(classification, rawText) {
  const { intent, summary, skill: skillName, params = {}, confidence, classifier } = classification;
  const actions = [];

  if (skillName && skills[skillName]) {
    actions.push(await skills[skillName].execute(params));
  }

  return {
    intent: intent || 'unknown',
    summary: summary || rawText.slice(0, 100),
    confidence: Number(confidence || 0),
    classifier,
    actions,
  };
}

app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    classifier: hasGeminiKey ? MODEL_NAME : 'fallback',
    fallbackAvailable: true,
    skills: Object.keys(skills),
  });
});

app.post('/process', async (req, res) => {
  const { rawText, entities = [] } = req.body || {};

  if (req.body?.image || req.body?.imageBase64 || req.body?.filePath) {
    return res.status(400).json({ error: 'image_not_allowed', message: 'Send extracted text only.' });
  }

  if (!rawText || typeof rawText !== 'string') {
    return res.status(400).json({ error: 'rawText_required', message: 'rawText must be a non-empty string.' });
  }

  if (!Array.isArray(entities)) {
    return res.status(400).json({ error: 'invalid_entities', message: 'entities must be an array.' });
  }

  try {
    const classification = await classifyIntent(rawText, entities);
    res.json(await buildResponse(classification, rawText));
  } catch (err) {
    res.status(502).json({ error: 'classification_failed', message: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`OpenClaw Gateway listening on http://localhost:${PORT}`);
  console.log(`Classifier: ${hasGeminiKey ? MODEL_NAME : 'fallback'}`);
});
