# GERAI UNA SMART POS AI

Aplikasi Kasir Android (Jetpack Compose + Room Offline-First + Firebase Auth & Cloud Firestore Sync).

---

## Download APK

[![Download dari Releases](https://img.shields.io/badge/Download%20APK-GitHub%20Releases-0F766E?style=for-the-badge&logo=github&logoColor=white)](../../releases)
[![Lihat Build Actions](https://img.shields.io/badge/Build%20Status-GitHub%20Actions-1E293B?style=for-the-badge&logo=githubactions&logoColor=white)](../../actions)

### Cara Unduh APK Terbaru:
1. Buka tab **[Releases](../../releases)** pada repositori ini.
2. Pilih rilis **Gerai Una POS AI (Latest APK)** → bagian **Assets** → unduh **`gerai-una-pos-ai.apk`**.
3. Alternatifnya, buka tab **[Actions](../../actions)** → pilih workflow **Build & Publish APK** terbaru → unduh artifact **`gerai-una-pos-ai-apk`**.
4. Instal file **`gerai-una-pos-ai.apk`** di perangkat Android Anda.

> **Catatan Cloud Sync (Opsional):** Saat *Push to GitHub* dari AI Studio, file `app/google-services.json` tidak ikut terunggah demi keamanan. Build APK GitHub tetap berjalan normal menggunakan **Mode Offline (Room DB)**. Jika ingin mengaktifkan Login Google & Cloud Sync di APK hasil build GitHub, tambahkan GitHub Repository Secret bernama **`GOOGLE_SERVICES_JSON`** berisi isi file `app/google-services.json`, lalu jalankan ulang workflow.

