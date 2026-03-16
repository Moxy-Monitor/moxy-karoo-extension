package com.fortioridesign.moxykaroo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.ActiveRideProfile
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.SavedDevices
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform

fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
            trySendBlocking(event.state)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamRideState(): Flow<RideState> {
    return callbackFlow {
        val listenerId = addConsumer { rideState: RideState ->
            trySendBlocking(rideState)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamSavedDevices(): Flow<SavedDevices> {
    return callbackFlow {
        val listenerId = addConsumer { savedDevices: SavedDevices ->
            trySendBlocking(savedDevices)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamActiveRideProfile(): Flow<ActiveRideProfile> {
    return callbackFlow {
        val listenerId = addConsumer { activeProfile: ActiveRideProfile ->
            trySendBlocking(activeProfile)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

fun KarooSystemService.streamUserProfile(): Flow<UserProfile> {
    return callbackFlow {
        val listenerId = addConsumer { userProfile: UserProfile ->
            trySendBlocking(userProfile)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}

inline fun<reified T> encodeJson(t: T): String {
    return java.util.Base64.getEncoder().encodeToString(jsonWithUnknownKeys.encodeToString(t).toByteArray(Charsets.UTF_8))
}

inline fun<reified T> decodeJson(s: String): T {
    val jsonString = String(java.util.Base64.getDecoder().decode(s), Charsets.UTF_8)
    return jsonWithUnknownKeys.decodeFromString(jsonString)
}

fun<T> Flow<T>.throttle(timeout: Long): Flow<T> = this
    .conflate()
    .transform {
        emit(it)
        delay(timeout)
    }