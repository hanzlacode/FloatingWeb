🚀 FloatingWeb – Multi-Chart Trading Browser + Local Alert System

Advanced Android Overlay App for Traders (Unlimited Charts, Binance WebSocket Alerts, Floating Windows, JS Injection)

FloatingWeb is a high-performance multi-chart floating browser designed for professional traders.
It lets you open unlimited TradingView charts inside movable, resizable overlay windows—running on top of all apps.
It also includes a full local alert engine powered by Binance WebSocket + REST validation, custom alerts, and foreground price monitoring.

Built using Kotlin, Jetpack Compose, WindowManager, Foreground Services, and Binance APIs.

🌟 Key Highlights

✔ Unlimited multi-chart floating windows

Create as many overlay windows as you want — each powered by a dedicated container inside a single service.

✔ 3 Window Modes

Full Size – Full chart trading mode

Mini Size – Small draggable chart

Minimize/Hidden – Bubble mode, keeps running in background

✔ Overlay always-on-top

Charts stay above all apps (WhatsApp, Chrome, Binance, etc.).

✔ Binance Local Alert System

Runs in a separate foreground service inside your app:

Price Above X

Price Below X

Supports COIN-M, USD-M, SPOT

Live WebSocket ticker

Auto reconnect

Custom alert sound

Validates trading pairs using Binance REST API

✔ Save Everything Per Window

Each floating window remembers:

Chart URL

Selected timeframe

Window state (Full / Mini / Hidden)

User-added custom buttons

Last session history

Stored using SharedPreferences (lightweight local persistence).

✔ Resizable, Draggable, Multi-Mode Windows

Highly optimized WindowManager implementation:

Resize by edges

Drag anywhere on screen

Smooth UI frame rate

Zero lag

✔ TradingView Powered

Optimized for TradingView, but can load any website for chart reference.

✔ Risk Calculator Included

Quick risk management panel integrated inside the overlay.

✔ JS Injection Engine

Injects JavaScript into WebView for:

Parsing price

Capturing data
(Current live price now uses Binance WS for accuracy)

🛠️ Technical Architecture (Strong Professional Explanation)

▶ Multi-Overlay Engine

Built with WindowManager

Each floating container is identified dynamically

Runs inside a single foreground service (high performance)

Supports unlimited windows → low memory footprint

▶ Services

FloatingBrowserService

Creates and manages overlay windows

Handles states + drag + resize + layout transitions

PriceAlertService

Maintains Binance WebSocket

Auto-reconnects

Foreground persistent mode

▶ Network Layer

Binance WebSocket: Live ticker for alerts

Binance REST API: Validate symbol (COIN-M, USD-M, SPOT)

OKHttp + Gson for parsing

▶ Storage

SharedPreferences for:

Chart links

Window state

Buttons

Timeframes

▶ UI Layer

Jetpack Compose

Material 3  

Compose state management

📸 Screenshots
<img width="417" height="963" alt="Screenshot 2025-11-25 200715" src="https://github.com/user-attachments/assets/9653c7f4-1da4-451a-9192-1c833585f431" />
<img width="417" height="963" alt="Screenshot 2025-11-25 200625" src="https://github.com/user-attachments/assets/412d6ed2-7645-4c29-a0bf-228db7ae7a74" />
<img width="417" height="963" alt="Screenshot 2025-11-25 200520" src="https://github.com/user-attachments/assets/1b7a0ad9-7444-4547-a5d1-a03ccd73ced8" />
<img width="417" height="963" alt="Screenshot 2025-11-25 200450" src="https://github.com/user-attachments/assets/7b18e897-4273-47bf-9a7f-3493b7f2ea2d" />
<img width="417" height="963" alt="Screenshot 2025-11-25 200441" src="https://github.com/user-attachments/assets/0c4c35ad-36a5-474d-96ba-0b472e46de0c" />
<img width="417" height="963" alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/6769d5db-9e7f-4510-91ff-d84ab0a447fe" />


🔐 Permissions Used
Your manifest includes:

SYSTEM_ALERT_WINDOW — Overlay floating windows

FOREGROUND_SERVICE — Persistent background operation

INTERNET — TradingView + Binance API

ACCESS_NETWORK_STATE

WAKE_LOCK — Keep service alive

POST_NOTIFICATIONS

REQUEST_IGNORE_BATTERY_OPTIMIZATIONS


integration

🧑‍💻 Author

Md Hanzla Tanweer
Android & Full-Stack Developer
📧 Email: hanzla.code@gmail.com 

📝 License

This project is currently private-use only.
(If you want MIT/Apache license, tell me — I’ll add it.)



