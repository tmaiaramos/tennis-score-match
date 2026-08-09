package com.tennis.matchscore.data.local.converter

import androidx.room.TypeConverter
import com.tennis.matchscore.domain.model.DominantHand
import com.tennis.matchscore.domain.model.MatchStatus
import com.tennis.matchscore.domain.model.TrackingLevel

class Converter {

    @TypeConverter
    fun fromMatchStatus(status: MatchStatus): String = status.name

    @TypeConverter
    fun toMatchStatus(value: String): MatchStatus = runCatching {
        MatchStatus.valueOf(value)
    }.getOrDefault(MatchStatus.IN_PROGRESS)

    @TypeConverter
    fun fromTrackingLevel(level: TrackingLevel): String = level.name

    @TypeConverter
    fun toTrackingLevel(value: String): TrackingLevel = runCatching {
        TrackingLevel.valueOf(value)
    }.getOrDefault(TrackingLevel.BASIC)

    @TypeConverter
    fun fromDominantHand(hand: DominantHand?): String? = hand?.name

    @TypeConverter
    fun toDominantHand(value: String?): DominantHand? = value?.let {
        runCatching { DominantHand.valueOf(it) }.getOrNull()
    }
}