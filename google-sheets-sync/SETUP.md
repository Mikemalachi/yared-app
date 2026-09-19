# Google Sheets sync — one-time setup (about 5 minutes)

## 1. Make the sheet and paste the script
1. Go to sheets.google.com and create a blank spreadsheet (name it e.g. "Yared Hymns").
2. Menu **Extensions → Apps Script**. Delete whatever is in `Code.gs` and paste the whole
   contents of `Code.gs` from this folder. Click **Save** (disk icon).
3. In the function dropdown at the top pick **setup** and click **Run**.
   Google will ask for permission ("Review permissions" → your account → Advanced →
   "Go to project (unsafe)" → Allow). This is your own script; it needs access to the
   sheet and to a Drive folder for lyric images.
   Back in the sheet you'll see a **Hymns** tab with the columns and a popup with your **secret key**.

## 2. Publish it as a web app
1. In Apps Script: **Deploy → New deployment** → gear icon → **Web app**.
2. Execute as: **Me**. Who has access: **Anyone**. Click **Deploy**.
3. Copy the **Web app URL** (ends in `/exec`).

"Anyone" only means the URL is reachable; every request must carry your secret key,
so nobody can read or change your sheet without it.

## 3. Connect the app — scan the QR
1. In the sheet, menu **Yared Sync → Show app connection info**.
2. Paste the Web app URL into the box at the top — a QR code appears (the URL is remembered for next time).
3. In the app: Home → **Google Sheets sync → 📷 Scan QR to connect**, and point the camera at it.
   It connects and runs the first sync straight away.

No camera handy (e.g. on a PC)? In the scan window, pick a screenshot of the QR, or paste the
connection code. Already connected on one device? Tap **Show QR** there and scan it from the other.
Typing the URL and secret key by hand under **Connection** still works too.

## Tabs
One tab per house — **ንባብ ቤት, ዜማ ቤት, ቅዳሴ ቤት, አቋቋም ቤት, ትርጓሜ ቤት** — plus **Unsorted** (hymns not filed yet)
and **Celebrations** (background pictures). The **folder** column holds the path inside the house, e.g.
`ድጓ / ጥር / 11`. Rows are kept sorted by folder, then title. To move a hymn, change its folder text, or cut
the row and paste it into another house's tab — the app follows on the next sync.
(An older single "Hymns" tab is renamed to Unsorted automatically.)

## How it behaves
| Column | Direction |
|---|---|
| title, category, month, celebration, audioLink | both ways — whichever side changed it since the last sync wins; if both changed, the newer edit wins |
| status (New / Needs Practice / Learning / Well Memorized) | both ways — the phone wins if both changed |
| practiceCount | both ways — the higher number is kept |
| length, lastPracticed | phone → sheet |
| lyricImageLinks | both ways — phone images are uploaded to the Drive folder "Yared Hymn Tracker Lyrics"; links you paste into the sheet (Drive or any public image URL, one per line) are downloaded to the phone; removing a link removes that page |
| lyricPreview | shows page 1 of the lyrics inside the sheet |
| deleted | tick it to delete a hymn from the phone on its next sync; hymns deleted on the phone get ticked here |

**Celebrations tab** — one row per celebration with its background picture (`imageLink`, a Drive or any image link) and a preview. Pictures set in the app are uploaded here and appear on your other devices; clearing a link removes that picture everywhere. The newest change wins.

* Add a hymn from the sheet by typing a new row with at least a title — an id is filled in automatically.
* Don't delete rows in the sheet; tick **deleted** instead (a missing row is simply put back from the phone).
* Audio stored only on the phone is not uploaded; add a link in **audioLink** if you want it in the sheet.
* The app syncs automatically when opened (and when you return to it after 15+ minutes) if "Auto-sync" is on.

## If you edit Code.gs later
Deploy → **Manage deployments** → pencil → Version: **New version** → Deploy. The URL stays the same.
