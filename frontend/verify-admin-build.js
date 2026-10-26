const { spawnSync } = require("child_process");

const result = spawnSync("npm", ["run", "build"], {
  cwd: __dirname,
  stdio: "inherit",
  shell: process.platform === "win32",
});

process.exit(result.status ?? 1);