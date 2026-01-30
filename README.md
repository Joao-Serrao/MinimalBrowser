# MinimalBrowser

# 

##### &nbsp;A lightweight, distraction-free Android browser built around WebView, designed for fast searching, split-screen browsing, and immersive full-screen use.

# 

##### &nbsp;This project focuses on minimal UI, gesture-based controls, and power-user features while staying simple and self-contained.

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

##### &nbsp;-Quick search input with keyboard support

##### 

##### &nbsp;-Home button instantly returns to the DuckDuckGo homepage

##### 

##### &nbsp;-Optional animated hide/show search bar for maximum screen space

# 

### Split WebView Mode

# 

##### &nbsp;-Toggle split screen to browse two pages at once

##### 

##### &nbsp;-Works in portrait (vertical split) and landscape (horizontal split)

##### 

##### &nbsp;-Long-press split button to:

##### 

##### &nbsp;-Open a second WebView instantly

##### 

##### &nbsp;-Swap left/right WebViews

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

##### &nbsp;-Basic ad \& tracker blocking via request interception

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

##### │   ├── Gesture handling

##### │   ├── Keyboard \& immersive mode control

##### │

##### ├── WebViewHolder.java

##### │   ├── Centralized WebView creation

##### │   ├── WebView settings \& clients

##### │   ├── Fullscreen video handling

##### │   └── Basic ad blocking

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

##### &nbsp;-Drag anywhere on screen to resize panes

##### 

##### &nbsp;-Double-tap again to exit

# 

## Privacy \& Security Notes

# 

##### &nbsp;-No user data is collected

##### 

##### &nbsp;-No analytics or tracking SDKs

##### 

##### &nbsp;-Cookies are enabled only for WebView compatibility

##### 

##### &nbsp;-Third-party trackers and common ad domains are blocked at request level

# 

## Requirements

# 

##### &nbsp;-Android API level compatible with WebView

##### 

##### &nbsp;-Internet permission

##### 

##### &nbsp;-Tested on modern Android versions (Android 13+ recommended)

# 

## Possible Improvements

# 

##### &nbsp;-URL bar support

##### 

##### &nbsp;-Tab management

##### 

##### &nbsp;-Custom search engines

##### 

##### &nbsp;-Advanced content blocking

##### 

##### &nbsp;-Dark mode toggle

# 

##### &nbsp;-Gesture navigation (swipe back/forward)

# 

## License

# 

##### &nbsp;This project is provided as-is for personal or educational use.

