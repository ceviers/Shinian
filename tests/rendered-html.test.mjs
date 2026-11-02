import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

const projectRoot = new URL("../", import.meta.url);

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("server-renders the Glitter product prototype", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>拾念 · 本地加密灵感记录<\/title>/i);
  assert.match(html, /开始使用拾念/);
  assert.match(html, /创建本机资料库/);
  assert.match(html, /从加密备份恢复/);
  assert.match(html, /加密备份支持跨设备恢复/);
  assert.doesNotMatch(html, /codex-preview|Building your site|react-loading-skeleton/i);
});

test("keeps the prototype aligned with implemented Android flows", async () => {
  const [page, css, layout, packageJson] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../package.json", import.meta.url), "utf8"),
  ]);

  assert.match(page, /App 私有加密目录/);
  assert.match(page, /确认删除/);
  assert.match(page, /播放录音/);
  assert.match(page, /导入加密备份/);
  assert.match(page, /function FullscreenImage/);
  assert.match(page, /function WebsiteView/);
  assert.match(css, /\.interactive-detail/);
  assert.match(css, /\.image-viewer/);
  assert.match(layout, /title:\s*"拾念 · 本地加密灵感记录"/);
  assert.match(packageJson, /"build": "vinext build"/);
  await assert.rejects(access(new URL("app/_sites-preview", projectRoot)));
});
