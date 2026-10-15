# 📱 IP Address & Subnet Calculator

Aplikasi Android untuk menghitung subnet IPv4 dan IPv6 dengan tampilan Material Design, mendukung **Light Mode** dan **Dark Mode**.

---

## ✨ Fitur

- ✅ Kalkulasi subnet **IPv4** (Network ID, Broadcast, Subnet Mask, Wildcard, Host range, CIDR)
- ✅ Kalkulasi **IPv6** (Expanded address, Network prefix, Address range, Total IP)
- ✅ Input prefix CIDR langsung (IPv4: /0–/32, IPv6: /0–/128)
- ✅ Validasi input real-time
- ✅ Toggle **Dark Mode / Light Mode** manual
- ✅ Preferensi tema tersimpan otomatis

---

## 📋 Persyaratan Perangkat

| Komponen | Minimum |
|---|---|
| Android | **7.0 (Nougat) / API 24** |
| RAM | 1 GB |
| Storage | ~5 MB |
| Arsitektur | ARM / ARM64 / x86 |

---

## 🚀 Cara Install APK

Aplikasi ini sudah siap diinstal tanpa perlu melakukan proses build atau coding.

### Langkah 1 — Download File APK

Klik tautan di bawah ini untuk langsung mengunduh aplikasinya:

👉 **[Download IP Calculator.apk](app/release/IP%20Calculator.apk)** 👈

*(Catatan: Jika link di atas tidak berfungsi di perangkat Anda, buka folder `app/release/` secara manual dan unduh file `IP Calculator.apk`).*

---

### Langkah 2 — Pindahkan APK ke HP Android (Jika download via PC)

Jika Anda mengunduh file APK tersebut melalui komputer/laptop, pindahkan ke HP dengan cara:
- **Via USB:** Sambungkan HP ke PC, copy file ke folder Download di HP.
- **Via Chat/Drive:** Kirim file APK ke diri sendiri via WhatsApp/Telegram/Google Drive, lalu download di HP.

*(Jika Anda langsung mengunduh dari browser HP pada Langkah 1, lewati langkah ini).*

---

### Langkah 3 — Aktifkan "Install dari Sumber Tidak Dikenal"

> ⚠️ Langkah ini wajib karena aplikasi tidak diunduh langsung dari Google Play Store.

**Android 8.0 ke atas:**
1. Buka **Pengaturan → Aplikasi**
2. Cari aplikasi **File Manager** (atau browser yang kamu pakai untuk download)
3. Ketuk → **Izin → Instal aplikasi tidak dikenal → Izinkan**

**Android 7.0:**
1. Buka **Pengaturan → Keamanan**
2. Aktifkan opsi **"Sumber tidak dikenal"**

---

### Langkah 4 — Install APK

1. Buka aplikasi **File Manager** di HP
2. Navigasi ke folder **Download**
3. Ketuk file **`IP Calculator.apk`**
4. Ketuk **"Install"**
5. Tunggu beberapa detik → ketuk **"Buka"**

---

## 🎮 Cara Pakai Aplikasi

### Mode IPv4
```text
1. Pastikan tab "IPv4" aktif (biru)
2. Isi kolom "IP Address"  → contoh: 192.168.1.10
3. Isi kolom "Prefix Length / CIDR" → contoh: 24
4. Ketuk tombol CALCULATE
5. Hasil tampil di bawah:
   • Network ID     → 192.168.1.0
   • Broadcast      → 192.168.1.255
   • Subnet Mask    → 255.255.255.0
   • Wildcard Mask  → 0.0.0.255
   • Usable Hosts   → 254
   • First Host     → 192.168.1.1
   • Last Host      → 192.168.1.254
   • CIDR Prefix    → /24

1. Ketuk tab "IPv6"
2. Isi kolom "IPv6 Address" → contoh: 2001:db8::1
3. Isi kolom "Prefix Length / CIDR" → contoh: 32
4. Ketuk tombol CALCULATE
5. Hasil tampil di bawah:
   • Expanded Address → 2001:0DB8:0000:...
   • Network Prefix   → 2001:0DB8::/32
   • Address Range    → first – last
   • Total IPs        → 2^96 (dengan notasi)

Ketuk ikon 🌙 / ☀️ di pojok kanan header
→ Tema berubah dan tersimpan otomatis

# Clone / buka folder project
cd "ip calc"

# Build debug APK
./gradlew assembleDebug

# File APK hasil build ada di:
# app/build/outputs/apk/debug/app-debug.apk

ip calc/
├── app/
│   ├── release/
│   │   └── IP Calculator.apk         ← File aplikasi siap install (Direct Link)
│   └── src/main/
│       ├── java/com/ipcalc/app/
│       │   ├── MainActivity.kt       ← UI controller + dark mode toggle
│       │   ├── IPv4Calculator.kt     ← Kalkulasi subnet IPv4
│       │   ├── IPv6Calculator.kt     ← Kalkulasi subnet IPv6
│       │   └── InputValidator.kt     ← Validasi input
│       └── res/
│           ├── layout/               ← XML layout
│           └── values/               ← Tema aplikasi
├── build.gradle
└── settings.gradle

