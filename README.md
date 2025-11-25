# 🚀 FloatingWeb – Multi-Chart Trading Browser & Local Alert System

**Advanced Android Overlay App for Traders**  
Kotlin • Jetpack Compose • WindowManager • Binance WebSocket • JS Injection • Foreground Services

FloatingWeb is a high-performance multi-chart floating browser designed for professional traders.  
It allows you to open **unlimited TradingView charts** inside movable, resizable floating windows that run on top of all apps, with a full **local alert engine** powered by Binance WebSocket + REST validation.

---

## ✨ Features

### 🔹 Unlimited Floating Charts
- Open multiple floating windows  
- Resize, drag, hide/minimize  
- Modes: **Full**, **Mini**, **Hidden**  

### 🔹 Binance Local Alert System
- Price Above X  
- Price Below X  
- Supports **COIN-M / USD-M / SPOT**  
- Live WebSocket ticker  
- Auto-reconnect  
- Custom alert sounds  
- Symbol validation using Binance REST API  

### 🔹 Per-Window Data Persistence
- Stored locally using SharedPreferences
- Chart URL  
- Selected timeframe  
- Window mode  
- User-added custom buttons  
- Last session history  

### 🔹 Floating Window Engine
- WindowManager overlay  
- Smooth drag + resize  
- Always-on-top behavior  
- Optimized for low latency  

### 🔹 TradingView Optimised
- Fully tuned for TradingView charts  
- Also supports any other website  

### 🔹 Built-In Risk Calculator
- Quick position sizing  
- Basic risk analysis inside the overlay  

### 🔹 JS Injection Engine
- Injects JavaScript into WebView  
- For DOM extraction and utilities  
- (Live price now handled by Binance WebSocket)

---

## 🛠 Tech Stack
- Kotlin  
- Jetpack Compose  
- WindowManager Overlays  
- Foreground Services  
- Binance WebSocket + REST  
- OkHttp + Gson  
- SharedPreferences  
- WebView + JS Injection  

---

## 🧩 Architecture Overview

### ▶ FloatingBrowserService
- Creates and manages floating windows  
- Handles drag, resize, and mode switching  

### ▶ PriceAlertService
- Maintains Binance WebSocket connection  
- Auto-reconnect system  
- Validates trading pairs via REST  

### ▶ Network Layer
- WebSocket for ticker stream  
- REST API for symbol validation  

### ▶ UI Layer
- Compose UI  
- Material 3 components  
- State-driven architecture  

---

## 🔐 Permissions
- SYSTEM_ALERT_WINDOW  
- FOREGROUND_SERVICE  
- FOREGROUND_SERVICE_DATA_SYNC  
- INTERNET  
- ACCESS_NETWORK_STATE  
- WAKE_LOCK  
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS  
- POST_NOTIFICATIONS  

---


## 📸 Screenshots

<img width="167"  alt="Screenshot 2025-11-25 200429" src="https://github.com/user-attachments/assets/cef99e72-18f2-4926-a8ce-8db51c23e87a" />
<img width="167"  alt="Screenshot 2025-11-25 200450" src="https://github.com/user-attachments/assets/3a2751f5-ffce-477f-abfb-2e710dd1b5a3" />
<img width="167"  alt="Screenshot 2025-11-25 200441" src="https://github.com/user-attachments/assets/8f8db952-c8cc-457c-a7cb-f9763d097cfa" />
<img width="167"  alt="Screenshot 2025-11-25 200625" src="https://github.com/user-attachments/assets/fab60791-0dde-425f-868b-f9fff4558c10" />
<img width="167"  alt="Screenshot 2025-11-25 200715" src="https://github.com/user-attachments/assets/fe286552-548f-4d75-8b35-15828f921852" />
<img width="167"  alt="Screenshot 2025-11-25 200520" src="https://github.com/user-attachments/assets/1f13ceec-11f6-4c0d-8497-f64e73e9ebd8" />


## 📱 APK Download
- https://github.com/hanzlacode/FloatingWeb/releases/tag/v1.0.0
---

## 🎯 Current Focus
- Freelance Android & Web development  
- Automation tools & AI-powered features  
- Shipping production-ready applications  

---

## 📫 Contact
**Email:** hanzla.code@gmail.com

