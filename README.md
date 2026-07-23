# MinimalBrowser

# 

##### &nbsp;A lightweight, distraction-free Android browser built around WebView, designed for fast searching, split-screen browsing, and immersive full-screen use.

# 

##### &nbsp;This project focuses on minimal UI, gesture-based controls, and power-user features while staying simple and self-contained.

# 

## Download

[![Latest release](https://img.shields.io/github/v/release/Joao-Serrao/MinimalBrowser?label=latest&color=3ddc84)](https://github.com/Joao-Serrao/MinimalBrowser/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Joao-Serrao/MinimalBrowser/total?color=3ddc84)](https://github.com/Joao-Serrao/MinimalBrowser/releases)

**[⬇️ Download the latest APK](https://github.com/Joao-Serrao/MinimalBrowser/releases/latest/download/MinimalBrowser.apk)** &nbsp;·&nbsp; [All releases](https://github.com/Joao-Serrao/MinimalBrowser/releases) &nbsp;·&nbsp; [Download page](https://joao-serrao.github.io/MinimalBrowser/)

Requires **Android 7.0 (API 24)** or newer.

1. Download the APK on your phone and open it.
2. Android will ask permission to install from this source — allow it (the app is not on the Play Store, so it is flagged as an unknown source).
3. Tap **Install**. For the widget: long-press the home screen → **Widgets** → **Minimal Browser**.

Prefer to build it yourself? `./gradlew assembleRelease` — see [Building](#building).

# 

## Motivation



##### &nbsp;This app was created as a replacement for the default Google search widget.

##### 

##### &nbsp;I wanted a home screen widget that opens a simple, built-in browser instead of launching Chrome, DuckDuckGo Browser, or any other external app. The goal was quick searching and browsing without:

##### 

##### &nbsp;-Being forced into a full-featured browser

##### 

##### &nbsp;-Opening external browser apps

##### 

##### &nbsp;-Using traditional tab-based navigation

##### 

##### Instead of tabs, this app uses split WebViews and focus-based interaction, making it faster and more lightweight for quick lookups and multitasking.





## Features



### Search-First Design

# 

##### &nbsp;-DuckDuckGo as the default search engine (privacy-focused)

##### 

##### &nbsp;-Smart address bar: type a URL (or domain, localhost, IP) to go straight there, anything else runs a search

##### 

##### &nbsp;-The search field doubles as an omnibox, showing the current page's URL (or the DuckDuckGo query)

##### 

##### &nbsp;-Quick search input with keyboard support

##### 

##### &nbsp;-Home button instantly returns to the DuckDuckGo homepage and clears the address bar

##### 

##### &nbsp;-Optional animated hide/show search bar for maximum screen space

##### 

##### &nbsp;-Swipe left-to-right across the search bar to go back (same as the system Back button)

# 

### Split WebView Mode

# 

##### &nbsp;-Toggle split screen to browse two pages at once

##### 

##### &nbsp;-Works in portrait (vertical split) and landscape (horizontal split)

##### 

##### &nbsp;-Long-press the split button:

##### 

##### &nbsp;-When the second window is closed, it opens a second WebView on Claude (claude.ai), replacing whatever it last showed

##### 

##### &nbsp;-When the second window is already open, it swaps the left/right WebViews

##### 

##### &nbsp;-Double-tap a WebView to enter resize mode

##### 

##### &nbsp;-Drag to resize panes dynamically

##### 

##### &nbsp;-Double-tap again to exit resize mode

##### 

##### &nbsp;-Layout weights are preserved across orientation changes

# 

### Active WebView Handling

# 

##### &nbsp;-Automatically tracks which WebView is active

##### 

##### &nbsp;-Searches, navigation, and back actions apply only to the active view

##### 

##### &nbsp;-Tap-to-focus behavior for intuitive multitasking

##### 

##### &nbsp;-Opening the split keeps focus on the window you were already using, so Back keeps driving it

# 

### Immersive Full-Screen Experience

# 

##### &nbsp;-True immersive mode (status bar \& navigation hidden)

##### 

##### &nbsp;-Sticky immersive flags for uninterrupted browsing

##### 

##### &nbsp;-Fullscreen video playback support via WebChromeClient

##### 

##### &nbsp;-Automatic orientation handling for media

# 

### Keyboard-Aware UI

# 

##### &nbsp;-Smart keyboard hide on outside tap

##### 

##### &nbsp;-Smooth layout resizing when keyboard opens

##### 

##### &nbsp;-Custom workaround for consistent keyboard behavior across devices

# 

### Home Screen Widget

# 

##### &nbsp;-Search widget for instant access

##### 

##### &nbsp;-Tapping the widget opens the app and focuses the search input

##### 

##### &nbsp;-Lightweight AppWidgetProvider implementation

# 

### WebView Enhancements

# 

##### &nbsp;-Modern mobile user agent

##### 

##### &nbsp;-JavaScript, DOM storage, and cookies enabled

##### 

##### &nbsp;-Ad \& tracker blocking via request interception: an ad/tracker domain list, whole-DNS-label matching (ads./adserver./analytics./tracking. on any domain), and ad path fragments

##### 

##### &nbsp;-Redirect-to-ad navigations are cancelled, and popups/new windows are suppressed

##### 

##### &nbsp;-Page load progress bar, and a themed error page with Retry on load failure

##### 

##### &nbsp;-File uploads (\<input type="file"\>) and JavaScript alert/confirm dialogs supported

##### 

##### &nbsp;-External app handling for non-HTTP(S) URLs

##### 

##### &nbsp;-Downloads delegated to system browser or download manager

# 

## Project Structure



##### minimalbrowser

##### │

##### ├── SearchActivity.java

##### │   ├── Main UI and navigation

##### │   ├── Split WebView logic

##### │   ├── Gesture handling (swipe-back, double-tap resize)

##### │   ├── Address bar / omnibox and load progress

##### │   ├── Keyboard \& immersive mode control

##### │

##### ├── WebViewHolder.java

##### │   ├── Centralized WebView creation

##### │   ├── WebView settings \& clients

##### │   ├── Fullscreen video handling

##### │   ├── Ad \& tracker blocking

##### │   └── File chooser, JS dialogs, error page

##### │

##### ├── UrlUtil.java

##### │   └── URL-vs-search detection and address bar text (unit-tested)

##### │

##### ├── SystemUi.java

##### │   └── Single definition of immersive mode

##### │

##### ├── DownloadsActivity.java / NotificationHelper.java

##### │   └── Download list and notifications

##### │

##### └── SearchWidget.java

##### &nbsp;   └── Home screen search widget

##### │

##### └── Others

#####     ├── xml to define the UI

#####     └── AndroidManifest.xml





## How It Works





### Search Flow

# 

##### &nbsp;1.User enters a query

##### 

##### &nbsp;2.Query is converted to a DuckDuckGo URL

##### 

##### &nbsp;3.Result loads in the currently active WebView

# 

### Split Mode Logic

# 

##### &nbsp;-First WebView is always created at launch

##### 

##### &nbsp;-Second WebView is created lazily when split mode is enabled

##### 

##### &nbsp;-Layout weights determine size and are user-adjustable

##### 

##### &nbsp;-Orientation changes automatically re-apply correct layout behavior

# 

### Resize Mode

# 

##### &nbsp;-Double-tap a WebView → resize mode

##### 

##### &nbsp;-Drag within the web area to resize panes (the top bar stays usable)

##### 

##### &nbsp;-Double-tap again to exit

# 

## Privacy \& Security Notes

# 

##### &nbsp;-No user data is collected

##### 

##### &nbsp;-No analytics or tracking SDKs

##### 

##### &nbsp;-Cookies are enabled for WebView compatibility, including third-party cookies (needed to stay signed in to sites such as Claude). A toggle for this is on the improvements list.

##### 

##### &nbsp;-Third-party trackers and common ad domains are blocked at request level

# 

## Requirements

# 

##### &nbsp;-Android 7.0 (API 24) or newer

##### 

##### &nbsp;-Internet permission

##### 

##### &nbsp;-Tested on modern Android versions (Android 13+ recommended)

# 

## Building

Clone the repo and build with the Gradle wrapper (JDK 17+ and the Android SDK required):

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew test              # unit tests
```

For a signed release build, create a `keystore.properties` in the project root
(it is gitignored, and the release build falls back to unsigned without it):

```properties
storeFile=release-keystore.jks
storePassword=your-store-password
keyAlias=your-alias
keyPassword=your-key-password
```

Generate the keystore once with:

```bash
keytool -genkeypair -v -keystore release-keystore.jks -alias your-alias \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then `./gradlew assembleRelease` produces a signed, R8-shrunk APK at
`app/build/outputs/apk/release/app-release.apk`.

> Keep the keystore and its password safe and backed up. Android will refuse an
> update signed with a different key.

# 

## Possible Improvements

# 

##### &nbsp;-Tab management

##### 

##### &nbsp;-Custom search engines and a configurable second-pane URL (currently DuckDuckGo and Claude)

##### 

##### &nbsp;-Advanced content blocking (filter-list support rather than a built-in domain list)

##### 

##### &nbsp;-Settings screen (ad-block toggle, desktop mode, third-party cookie toggle)

##### 

##### &nbsp;-Persist split state and open pages across process death

##### 

##### &nbsp;-Forward gesture (swipe right-to-left) to complement swipe-back

# 

## Support

# 

Minimal Browser is free and always will be. If you find it useful, you can
[buy me a coffee](https://www.buymeacoffee.com/joao.serrao) — appreciated but never expected.

# 

## License

# 

##### &nbsp;This project is provided as-is for personal or educational use.

