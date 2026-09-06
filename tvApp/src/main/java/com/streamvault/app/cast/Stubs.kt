package com.streamvault.app.cast
class CastManager @javax.inject.Inject constructor()
class CastMediaRequestFactory @javax.inject.Inject constructor()
class CastPlaybackCoordinator @javax.inject.Inject constructor()
class CastConnectionState
class CastMediaRequest
class CastMediaRequestBuildResult

sealed class CastPlaybackEvent {
    object MediaLoadSucceeded : CastPlaybackEvent()
    object MediaLoadFailed : CastPlaybackEvent()
    object SessionStartFailed : CastPlaybackEvent()
    object ReceiverUnavailable : CastPlaybackEvent()
    object RouteSelectionCancelled : CastPlaybackEvent()
}

class CastStartResult
