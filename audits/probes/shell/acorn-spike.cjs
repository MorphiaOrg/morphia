const fs = require('node:fs');
const acorn = require(process.argv[2]);
for (const row of fs.readFileSync(0,'utf8').trimEnd().split('\n')) {
  const [id, text] = row.split('\t');
  const raw = Buffer.from(text,'base64').toString('utf8');
  for (const mode of ['script','expression']) {
    let success = false, error = '';
    try {
      acorn.parse(mode==='expression' ? '('+raw+'\n)' : raw,
        {ecmaVersion:2026,sourceType:'script',locations:true,allowHashBang:false});
      success = true;
    } catch (failure) { error = failure.message; }
    process.stdout.write([id,mode,success,Buffer.from(error).toString('base64')].join('\t')+'\n');
  }
}
