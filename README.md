🚀 FloatingWeb – Multi-Chart Trading Browser + Local Alert System

Advanced Android Overlay App for Traders

Unlimited Charts • Binance WebSocket Alerts • Floating Windows • JS Injection • Foreground Services

FloatingWeb is a high-performance multi-chart floating browser for professional traders.
It allows you to open unlimited TradingView charts inside movable, resizable overlay windows that run on top of all apps.

It also includes a full local alert engine powered by Binance WebSocket + REST validation, custom alert sounds, and a dedicated foreground price-monitoring service.

Built with Kotlin, Jetpack Compose, WindowManager, Foreground Services, and Binance APIs.

🌟 Key Highlights

✔ Unlimited Multi-Chart Floating Windows

Open multiple overlay windows — each handled by a dynamic container inside a single high-performance service.

✔ Three Window Modes

Full Size – Full trading chart view

Mini Size – Draggable mini chart

Minimized / Hidden – Bubble mode

✔ Overlay Always On Top

Charts stay above all applications (Binance, Chrome, WhatsApp, etc.).

✔ Binance Local Alert System

Powered by a dedicated foreground service:

Price Above X

Price Below X

Supports COIN-M, USD-M, SPOT

Live WebSocket ticker

Auto-reconnect

Custom alert sounds

Symbol validation using Binance REST

✔ Per-Window Persistence

Each floating window saves:

Chart URL

Selected timeframe

Window state

Custom buttons

Last session history

Stored via SharedPreferences.

✔ Advanced Floating Window Engine

Highly optimized WindowManager implementation:

Edge-based resize

Drag anywhere on screen

Smooth animations

Zero frame drops

✔ TradingView Optimized

Primary support for TradingView charts
(Any website can be opened for reference use.)

✔ Risk Calculator Included

Built-in quick calculation panel for position sizing & risk management.

✔ JS Injection Engine

Inject JavaScript for:

Price parsing

DOM extraction

Advanced trading utilities

(Current live price uses Binance WebSocket for accuracy.)

🛠️ Technical Architecture

▶ Multi-Overlay Engine

WindowManager-based docking system

Dynamically indexed containers

Low-memory footprint

Managed inside one foreground service

▶ Core Services

FloatingBrowserService

Creates and manages all floating windows

Handles window states, drag, resize

PriceAlertService

Maintains Binance WebSocket

Handles auto-reconnect

Foreground high-priority mode

▶ Network Layer

Binance WebSocket (live ticker)

Binance REST API (symbol validation)

OkHttp + Gson

▶ Storage

SharedPreferences for UI state

Chart configurations

Window mode persistence

▶ UI Layer

Jetpack Compose

Material 3

State-driven UI

📸 Screenshots

<img width="417" height="963" src="https://github.com/user-attachments/assets/9653c7f4-1da4-451a-9192-1c833585f431" /> <img width="417" height="963" src="https://github.com/user-attachments/assets/412d6ed2-7645-4c29-a0bf-228db7ae7a74" /> <img width="417" height="963" src="https://github.com/user-attachments/assets/1b7a0ad9-7444-4547-a5d1-a03ccd73ced8" /> <img width="417" height="963" src="https://github.com/user-attachments/assets/7b18e897-4273-47bf-9a7f-3493b7f2ea2d" /> <img width="417" height="963" src="https://github.com/user-attachments/assets/0c4c35ad-36a5-474d-96ba-0b472e46de0c" /> <img width="417" height="963" src="https://github.com/user-attachments/assets/6769d5db-9e7f-4510-91ff-d84ab0a447fe" /> 

🔐 Permissions Used

Your Android manifest includes:

SYSTEM_ALERT_WINDOW — Overlay windows

FOREGROUND_SERVICE — Persistent live services

FOREGROUND_SERVICE_DATA_SYNC

INTERNET — Live trading data

ACCESS_NETWORK_STATE

WAKE_LOCK

REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

POST_NOTIFICATIONS

Binance package queries included for deep-linking support.

🧑‍💻 Author

Md Hanzla Tanweer
Android & Full-Stack Developer
📧 Email: hanzla.code@gmail.com 

📝 License

This project is currently private-use only.
(If you want MIT/Apache license, I can add it.)

