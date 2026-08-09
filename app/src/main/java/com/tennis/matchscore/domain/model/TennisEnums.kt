package com.tennis.matchscore.domain.model

// Níveis de marcação da partida
enum class TrackingLevel(val displayName: String) {
    BASIC("Básico"),
    INTERMEDIATE("Intermediário"),
    ADVANCED("Avançado")
}

// Mão dominante do jogador
enum class DominantHand(val displayName: String) {
    RIGHT_HANDED("Destro"),
    LEFT_HANDED("Canhoto")
}

// Tipo de Quadra
enum class CourtType(val displayName: String) {
    HARD("Quadra Rápida"),
    CLAY("Saibro")
}

// Tipos de Eventos / Pontos
enum class MatchEventType {
    ACE,
    DOUBLE_FAULT,
    WINNER,
    FORCED_ERROR,
    UNFORCED_ERROR
}

// Tipo do golpe / batida
enum class ShotType {
    GROUND,
    SLICE,
    SWING,
    VOLLEY,
    LOB,
    SMASH,
    DROP
}

// Mão do golpe
enum class HitHand {
    FOREHAND,
    BACKHAND
}

// Posição do jogador na quadra
enum class CourtPosition {
    BASELINE, // Fundo de quadra
    APPROACH, // Meio de quadra
    NET       // Rede
}

// Status da Partida
enum class MatchStatus {
    IN_PROGRESS, // Em andamento
    PAUSED,      // Pausada
    FINISHED     // Finalizada
}