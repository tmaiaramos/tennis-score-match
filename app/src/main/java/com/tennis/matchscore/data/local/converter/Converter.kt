package com.tennis.matchscore.data.local.converter

import androidx.room.TypeConverter
import com.tennis.matchscore.domain.model.CourtPosition
import com.tennis.matchscore.domain.model.CourtType
import com.tennis.matchscore.domain.model.DominantHand
import com.tennis.matchscore.domain.model.HitHand
import com.tennis.matchscore.domain.model.MatchEventType
import com.tennis.matchscore.domain.model.MatchStatus
import com.tennis.matchscore.domain.model.ServeState
import com.tennis.matchscore.domain.model.ShotType
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
    fun fromCourtType(type: CourtType): String = type.name

    @TypeConverter
    fun toCourtType(value: String): CourtType = runCatching {
        CourtType.valueOf(value)
    }.getOrDefault(CourtType.HARD)

    @TypeConverter
    fun fromDominantHand(hand: DominantHand?): String? = hand?.name

    @TypeConverter
    fun toDominantHand(value: String?): DominantHand? = value?.let {
        runCatching { DominantHand.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromServeState(serveState: ServeState): String = serveState.name

    @TypeConverter
    fun toServeState(value: String): ServeState = runCatching {
        ServeState.valueOf(value)
    }.getOrDefault(ServeState.FIRST_SERVE)

    @TypeConverter
    fun fromMatchEventType(type: MatchEventType): String = type.name

    @TypeConverter
    fun toMatchEventType(value: String): MatchEventType = runCatching {
        MatchEventType.valueOf(value)
    }.getOrDefault(MatchEventType.REGULAR_POINT)

    @TypeConverter
    fun fromCourtPosition(pos: CourtPosition?): String? = pos?.name

    @TypeConverter
    fun toCourtPosition(value: String?): CourtPosition? = value?.let {
        runCatching { CourtPosition.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromHitHand(hand: HitHand?): String? = hand?.name

    @TypeConverter
    fun toHitHand(value: String?): HitHand? = value?.let {
        runCatching { HitHand.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromShotType(type: ShotType?): String? = type?.name

    @TypeConverter
    fun toShotType(value: String?): ShotType? = value?.let {
        runCatching { ShotType.valueOf(it) }.getOrNull()
    }
}
