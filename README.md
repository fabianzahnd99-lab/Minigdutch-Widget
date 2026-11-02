# Mining-Dutch Widget (Android) – V1

Homescreen-Widget, das Hashrate, aktive Worker und Guthaben von Mining-Dutch anzeigt.
Aktualisiert sich alle 15 Minuten automatisch, oder sofort per ↻-Knopf.

## APK bauen

**Variante A – GitHub (ohne Installation am PC)**
1. Neues (privates) Repository auf github.com erstellen.
2. Den Inhalt dieses Ordners hochladen („Add file → Upload files“, Ordner `.github` mit hochladen).
3. Reiter **Actions** → „APK bauen“ läuft automatisch (ca. 3–5 Min.).
4. Unten beim Lauf das Artefakt **MiningDutchWidget-APK** herunterladen, entpacken, `app-debug.apk` aufs Handy.

**Variante B – Android Studio**
Ordner öffnen → warten bis Gradle synchronisiert → Handy per USB → ▶ Run
(oder Build → Build APK(s)).

## Einrichten
1. APK installieren („Installation aus unbekannten Quellen“ erlauben).
2. App öffnen, **API-Key** eintragen (mining-dutch.nl → eingeloggt → API Documentation).
3. Pool/Algorithmus eintragen, z. B. `sha256` für Bitcoin-ASICs.
4. **Testen** drücken → Hashrate mit der Website vergleichen.
   Liegt sie um Faktor 1000 daneben, die API-Einheit (Standard kH/s) anpassen.
5. **Speichern**, dann Widget auf den Homescreen ziehen.

## Datenquellen
`https://www.mining-dutch.nl/pools/{pool}.php?page=api&action=getuserstatus|getuserbalance|getuserworkers&api_key=…`
