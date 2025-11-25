🚀 FloatingWeb – Multi-Chart Trading Browser & Local Alert System

Advanced Android Overlay App for Traders
Kotlin • Jetpack Compose • WindowManager • Binance WebSocket • JS Injection • Foreground Services

FloatingWeb is a high-performance multi-chart floating browser built for traders who need real-time chart monitoring and custom alerts.
It supports unlimited TradingView windows, each running as an interactive floating overlay on top of all apps, with a fully local Binance price alert system.

✨ Features

🔹 Unlimited Floating Charts

Open multiple overlay windows

Resize, drag, hide/minimize

3 modes: Full • Mini • Hidden

🔹 Binance Local Alert Engine

Price Above X

Price Below X

Supports COIN-M / USD-M / SPOT

Live WebSocket ticker

Auto-reconnect

Custom alert sounds

Pair validation using Binance REST API

🔹 Per-Window Persistence

Stored using SharedPreferences:

Chart URL

Timeframe

Window state

Custom buttons

Last session history

🔹 Floating Window System

Powered by WindowManager

Smooth dragging & resizing

Always-on-top overlay

Zero lag / smooth UI updates

🔹 TradingView Optimized

Primary use-case for TradingView charts
(Any website can be loaded)

🔹 Built-in Risk Calculator

Quick position sizing & risk analysis tool.

🔹 JS Injection Engine

Inject scripts for:

DOM extraction

Price parsing
(Current prices use Binance WS for accuracy)

🛠 Tech Stack

Kotlin + Jetpack Compose

WindowManager Overlay System

Foreground Services

Binance WebSocket + REST

SharedPreferences

WebView + JS Injection

Material 3 • Compose UI

🧩 Architecture Overview

▶ FloatingBrowserService

Manages:

Window creation

Resize & drag handling

UI state transitions

▶ PriceAlertService

Maintains Binance WebSocket

Validates symbols

Auto-reconnect

Runs as persistent foreground service

▶ Network Layer

WebSocket (ticker stream)

REST API (pair validation)

OkHttp + Gson parsing

▶ UI Layer

Jetpack Compose

Material 3 components

State-driven architecture

📸 Screenshots

<div style="display: flex; flex-wrap: wrap; gap: 10px;"> <img src="https://github.com/user-attachments/assets/9653c7f4-1da4-451a-9192-1c833585f431" width="260"/> <img src="https://github.com/user-attachments/assets/412d6ed2-7645-4c29-a0bf-228db7ae7a74" width="260"/> <img src="https://github.com/user-attachments/assets/1b7a0ad9-7444-4547-a5d1-a03ccd73ced8" width="260"/> <img src="https://github.com/user-attachments/assets/7b18e897-4273-47bf-9a7f-3493b7f2ea2d" width="260"/> <img src="https://github.com/user-attachments/assets/0c4c35ad-36a5-474d-96ba-0b472e46de0c" width="260"/> <img src="https://github.com/user-attachments/assets/6769d5db-9e7f-4510-91ff-d84ab0a447fe" width="260"/> </div> 

🔐 Permissions

Used for core features:

SYSTEM_ALERT_WINDOW — Overlay windows

FOREGROUND_SERVICE — Background operations

INTERNET — Binance + TradingView

ACCESS_NETWORK_STATE

WAKE_LOCK — Keep service active

POST_NOTIFICATIONS

REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

📱 APK Download

You can try the latest version here:
👉 (Upload APK via GitHub Releases and paste your link here)

Guide (Do this once):

Go to your repo → Releases

Click Draft a new release

Tag version: v1.0.0

Upload your app-release.apk

Publish

Copy the link and paste above

🎯 Current Focus

Freelance Android + Web development

Building advanced automation + AI tools

Delivering production-level applications

📫 Contact
Email: hanzla.code@gmail.com 

Email: hanzla.code@gmail.com

