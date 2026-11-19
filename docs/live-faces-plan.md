# Live faces on wide tiles: plan

Status: **plan only, 19 November 2026.** Nothing here is built yet. Decisions marked **Q** need the owner.

## What exists today (0.2.1)

- A favourite tile can be one cell or, from its long-press menu, two cells wide (`wideFavouriteIds`).
- With live tiles on, a tile turns over on its top edge to show "faces", then turns back: front for 7 s, each face for 4 s, tiles staggered by 1.3 s. It stays still while pressed, with less motion, and when Meena isn't on screen (`LiveTileFlip.kt`).
- Faces come only from what Meena already reads on the phone (`LiveTiles.kt`):
  - **notifications:** the count, and for chats who they are from ("aisyah and 2 more"), never the message itself, and only when "show names" is on;
  - **calendar app:** the next event and when;
  - **alarm app:** the next alarm and its day.
- A face is two strings (`LiveFace(primary, secondary)`) laid out for a one-cell tile (about 73 dp). A wide tile shows the same face stretched, so the extra room is wasted.

Android has no way for other apps to send tile content to a launcher, so a face can only use data Meena already has. That stays the rule: no new permissions, no samples.

## Goal

A wide tile uses its extra width to say more about the same thing, in the Windows Phone wide-tile spirit: still text on a flat colour, square, lowercase, no icons in the text.

## What a wide face shows

| Source | One-cell face (today) | Wide face (proposed) |
|---|---|---|
| Chats | `3` / `aisyah and 2 more` | `3` big, then up to two senders on their own lines: `aisyah` / `farid and 1 more`. Names only when "show names" is on. Never message text. |
| Email apps | `2` / `new` | `2` big, then the newest sender. Subject only if **Q1** says yes. |
| Other apps' alerts | `5` / `new` | Unchanged. Their titles can be message text, so there's nothing safe to add. |
| Calendar app | `standup` / `today 09:00` | Title on one line, `today 09:00 to 09:30`, and the place if the event has one (`DeviceCalendarEvent.location`). |
| Alarm app | `06:30` / `alarm tomorrow` | `06:30` big, `tomorrow` and the alarm app's label, plus `in 9 h 20 m` while it's under 12 hours away. |
| Music app (new) | none | The playing track's title and artist, with the album cover filling the left square of the tile, from `MediaPlayerRepository` (already used by the inbox). Shown only while that app is the one playing. See **Q2**. |

Rules kept from today:
- Emoji are stripped (`PlainText.stripEmoji`), text is lowercase, captions at least 12 sp.
- No fake data: when there's nothing to say, the tile stays on its front.
- The touch target never moves; only the painted tile turns.

## Layout

- Wide tile: 2 cells + 6 dp gap, so about 152 × 73 dp.
- Big line top-left (count, time or title), secondary lines under it, left-aligned with a 6 dp inset, at most three lines in all.
- Music: cover as a 73 × 73 dp square on the left, title and artist on the right.
- The front of a wide tile is unchanged (icon and label).

## Motion

- Same flip and timings as one-cell tiles, so a row of mixed tiles feels like one surface.
- Option (**Q3**): wide tiles "peek" instead (the face slides up from the bottom, as Windows Phone wide tiles did), still off with less motion.

## Privacy (also fixes a gap that exists today)

- Shake to hide (`PrivacyVeil`) blurs chats, email, notes and money, but **live tile faces ignore it today**, so a sender's name can show on a tile while the inbox is blurred. With the veil on, tiles should stay on their front. This applies to one-cell tiles too.
- Wide faces add no new kind of data, so the privacy policy and Play data safety answers don't change, apart from the music cover (**Q2**), which also never leaves the phone.

## Build steps (once the questions are answered)

1. `LiveTiles.facesFor` gains a `wide: Boolean` and returns richer faces: `LiveFace` gets an optional third line and an optional image. Unit tests in `LiveTilesTest` for each source, wide and narrow, including names off and emoji.
2. `LiveTileFlip` lays out a wide face when the slot is wide (`FavouriteGrid` already knows).
3. Faces stay on the front while `PrivacyVeil.hidden` is on.
4. Music face, if **Q2** is yes: the tile whose package matches the playing session shows the cover face.
5. Peek motion, if **Q3** picks it.
6. Copy for any new words goes in `strings.xml`; check the 4.5:1 contrast for white text on each tile colour, including colourful tiles.

Roughly two working sessions: steps 1–3 first (no new data), then 4–5. No new libraries or permissions.

## Questions for the owner

- **Q1** Email subject on a wide tile: show it, or sender only? The subject is message content, which tiles have never shown.
- **Q2** Music face with the album cover on the music app's tile: yes or no?
- **Q3** Wide tiles flip like the others, or peek?
- **Q4** Should "show names" also cover the second sender line, or is one name enough?
