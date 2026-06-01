import { readFile } from 'node:fs/promises';

/**
 * @returns {Promise<Array<{ email: string, password: string, userId: string, conversationId: string }>>}
 */
export async function parseUsersCsv(csvPath, maxUsers = Infinity) {
  const text = await readFile(csvPath, 'utf8');
  const lines = text.split(/\r?\n/).filter((l) => l.trim().length > 0);
  if (lines.length < 2) {
    throw new Error(`CSV empty or missing data rows: ${csvPath}`);
  }

  const header = lines[0].split(',').map((h) => h.trim());
  const idx = {
    email: header.indexOf('email'),
    password: header.indexOf('password'),
    userId: header.indexOf('userId'),
    conversationId: header.indexOf('conversationId'),
  };
  for (const [key, i] of Object.entries(idx)) {
    if (i < 0) throw new Error(`CSV missing column: ${key}`);
  }

  const users = [];
  for (let i = 1; i < lines.length && users.length < maxUsers; i++) {
    const cols = lines[i].split(',');
    if (cols.length < 4) continue;
    users.push({
      email: cols[idx.email].trim(),
      password: cols[idx.password].trim(),
      userId: cols[idx.userId].trim(),
      conversationId: cols[idx.conversationId].trim(),
    });
  }
  return users;
}
