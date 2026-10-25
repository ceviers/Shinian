import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL("https://shinian-local-notes-prototype.ceviersmail.chatgpt.site"),
  title: "拾念 · 本地加密灵感记录",
  description: "为 Android 16 设计的本地优先随手记录 App 交互原型。",
  openGraph: {
    title: "拾念 · 把闪过的念头留在自己手里",
    description: "Android 16 本地加密灵感记录 App 交互原型",
    type: "website",
    locale: "zh_CN",
    images: [{ url: "/og.png", width: 1536, height: 1024, alt: "拾念 App 原型预览" }],
  },
  twitter: {
    card: "summary_large_image",
    title: "拾念 · 本地加密灵感记录",
    description: "Android 16 本地优先记录 App 交互原型",
    images: ["/og.png"],
  },
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
