package com.tvmime.tv.viewmodel

enum class TvOverlayState {
    HIDDEN,        // Pure fullscreen video playback
    HUD,           // Bottom banner (channel info, quick controls)
    MAIN_MENU,     // The main sliding side-navigation menu
    CHANNEL_LIST,  // Master channel list (left drawer)
    VOD,           // Movies/Series
    GUIDE,         // Full EPG Grid overlay
    CLOUD_SYNC,
    SETTINGS       // Settings pane overlay
}
