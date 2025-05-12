package com.francisco.raidorun

object Constants {
    const val INTERVAL_LOCATION = 4

    const val LIMIT_DISTANCE_ACCEPTED_BIKE = 0.04 * INTERVAL_LOCATION
    // LIMIT_DISTANCE_ACCEPTED correcto seria 0.04 (40m)
    // 40m son 144km/h. Un ciclista profesional en descenso podria alcanzar 130

    const val LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE = 0.035 * INTERVAL_LOCATION
    // LIMIT_DISTANCE_ACCEPTED correcto seria 0.035 (35m)
    // 35m son 126km/h.

    const val LIMIT_DISTANCE_ACCEPTED_RUNNING = 0.012 * INTERVAL_LOCATION
    // LIMIT_DISTANCE_ACCEPTED correcto seria 0.012 (12m)
    // 12m son 43.2km/h. Usain Bolt alcanza los 42km/h

    const val key_userApp = "USERAPP_KEY"
    const val key_provider = "PROVIDER_KEY"

    const val key_selectedSport = "SELECTEDSPORT_KEY"

    const val key_modeInterval = "MODEINTERVAL_KEY"
    const val key_intervalDuration = "INTERVALDURATION_KEY"
    const val key_progressCircularSeekBar = "PROGRESSCIRCULARSEEKBAR_KEY"
    const val key_maxCircularSeekBar = "MAXCIRCULARSEEKBAR_KEY"
    const val key_runningTime = "RUNNINGTIME_KEY"
    const val key_walkingTime = "WALKINGTIME_KEY"

    const val key_modeChallenge = "MODECHALLENGE_KEY"
    const val key_modeChallengeDuration = "MODECHALLENGEDURATION_KEY"
    const val key_challengeDurationHH = "CHALLENGEDURATIONHH_KEY"
    const val key_challengeDurationMM = "CHALLENGEDURATIONMM_KEY"
    const val key_challengeDurationSS = "CHALLENGEDURATIONSS_KEY"
    const val key_modeChallengeDistance = "MODECHALLENGEDISTANCE_KEY"
    const val key_challengeDistance = "CHALLENGEDISTANCE_KEY"

    const val key_challengeNotify = "NOTIFYCHALLENGE_KEY"
    const val key_challengeAutoFinish = "AUTOFINISH_KEY"
}