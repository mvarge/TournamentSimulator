const express = require('express');
const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = 3000;

const ROOT = path.join(__dirname, '..');
const CLASS_PATH = path.join(ROOT, 'out');
const DB_PATH = path.join(ROOT, 'databasefiles');

app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());

// List available leagues
app.get('/api/leagues', (req, res) => {
  const files = fs.readdirSync(DB_PATH)
    .filter(f => f.endsWith('.txt'))
    .map(f => ({
      id: f,
      name: f.replace('.txt', '').replace('_', ' ').toUpperCase()
    }));
  res.json(files);
});

// Run a simulation
app.post('/api/simulate', (req, res) => {
  const league = req.body.league || 'england.txt';

  // Sanitize: only allow filenames from the db folder
  const allowed = fs.readdirSync(DB_PATH).filter(f => f.endsWith('.txt'));
  if (!allowed.includes(league)) {
    return res.status(400).json({ error: 'Invalid league file' });
  }

  execFile(
    'java',
    ['-cp', CLASS_PATH, 'com.tournaments.Main', league, '--json'],
    { cwd: ROOT, timeout: 30000 },
    (err, stdout, stderr) => {
      if (err) {
        console.error('Java error:', stderr);
        return res.status(500).json({ error: 'Simulation failed', detail: stderr });
      }
      try {
        const data = JSON.parse(stdout.trim());
        res.json(data);
      } catch (parseErr) {
        console.error('JSON parse error:', parseErr, '\nOutput:', stdout);
        res.status(500).json({ error: 'Failed to parse simulation output' });
      }
    }
  );
});

app.listen(PORT, () => {
  console.log(`\n🏆 Tournament Simulator running at http://localhost:${PORT}\n`);
});
