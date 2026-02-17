const fs = require('fs');
const path = require('path');

const controllerPath = path.join(__dirname, 'src/controllers/householdController.js');
const source = fs.readFileSync(controllerPath, 'utf8');

const requiredSymbols = [
  'createHouseholdProfile',
  'getMyHouseholdProfile',
  'updateMyHouseholdProfile'
];

const missingDefinitions = requiredSymbols.filter((name) => {
  const fnDecl = new RegExp(`function\\s+${name}\\s*\\(`);
  const constDecl = new RegExp(`const\\s+${name}\\s*=\\s*async\\s*\\(`);
  return !fnDecl.test(source) && !constDecl.test(source);
});

const exportBlockMatch = source.match(/module\.exports\s*=\s*\{([\s\S]*?)\};/);
const exportBlock = exportBlockMatch ? exportBlockMatch[1] : '';
const missingExports = requiredSymbols.filter((name) => !new RegExp(`\\b${name}\\b`).test(exportBlock));

if (missingDefinitions.length || missingExports.length) {
  const details = [];
  if (missingDefinitions.length) details.push(`missing definitions: ${missingDefinitions.join(', ')}`);
  if (missingExports.length) details.push(`missing exports: ${missingExports.join(', ')}`);
  throw new Error(`Mobile household handlers check failed (${details.join(' | ')})`);
}

console.log('Mobile household controller definitions/exports look good ✅');
