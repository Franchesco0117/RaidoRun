package com.francisco.raidorun

/**
 * Constants
 *
 * This object holds global constant values used across the application.
 * It helps maintain clean and maintainable code by avoiding hardcoded strings
 * and values scattered throughout the project.
 *
 * It includes:
 * - Firebase collection names.
 * - SharedPreferences keys.
 * - Request codes for activities or permissions.
 * - General configuration flags or constants.
 *
 * Using this centralized object ensures consistency and simplifies updates.
 *
 * Author: [Francisco Castro]
 * Created: [8/MAY/2025]
 */
object Constants {
    /**
     * Time interval (in seconds) for location updates.
     */
    const val INTERVAL_LOCATION = 4

    /**
     * Maximum allowed distance (in km) for bike mode between location updates.
     *
     * Effective distance: 0.04 * 4 = 0.16 km (160 meters).
     * This equates to 144 km/h, which is acceptable for a professional cyclist on a descent.
     */
    const val LIMIT_DISTANCE_ACCEPTED_BIKE = 0.04 * INTERVAL_LOCATION

    /**
     * Maximum allowed distance (in km) for roller skating mode between location updates.
     *
     * Effective distance: 0.035 * 4 = 0.14 km (140 meters).
     * This represents a speed of approximately 126 km/h.
     */
    const val LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE = 0.035 * INTERVAL_LOCATION

    /**
     * Maximum allowed distance (in km) for running mode between location updates.
     *
     * Effective distance: 0.012 * 4 = 0.048 km (48 meters).
     * This equals 43.2 km/h, close to Usain Bolt's top sprinting speed.
     */
    const val LIMIT_DISTANCE_ACCEPTED_RUNNING = 0.012 * INTERVAL_LOCATION

    // ------------------------
    // Shared Preferences Keys
    // ------------------------

    /**
     * Key used to store the user object or session.
     */
    const val key_userApp = "USERAPP_KEY"

    /**
     * Key used to store the selected location provider.
     */
    const val key_provider = "PROVIDER_KEY"

    /**
     * Key used to store the selected sport (bike, run, rollerskate).
     */
    const val key_selectedSport = "SELECTEDSPORT_KEY"

    // Interval Mode Keys

    /**
     * Key indicating whether interval mode is enabled.
     */
    const val key_modeInterval = "MODEINTERVAL_KEY"

    /**
     * Key to store the duration (in seconds) of the interval.
     */
    const val key_intervalDuration = "INTERVALDURATION_KEY"

    /**
     * Key for the current progress of the circular seek bar (UI).
     */
    const val key_progressCircularSeekBar = "PROGRESSCIRCULARSEEKBAR_KEY"

    /**
     * Key for the maximum value of the circular seek bar.
     */
    const val key_maxCircularSeekBar = "MAXCIRCULARSEEKBAR_KEY"

    /**
     * Key to store the running time in interval mode.
     */
    const val key_runningTime = "RUNNINGTIME_KEY"

    /**
     * Key to store the walking time in interval mode.
     */
    const val key_walkingTime = "WALKINGTIME_KEY"

    // Challenge Mode Keys

    /**
     * Key indicating whether challenge mode is enabled.
     */
    const val key_modeChallenge = "MODECHALLENGE_KEY"

    /**
     * Key for the total challenge duration in seconds.
     */
    const val key_modeChallengeDuration = "MODECHALLENGEDURATION_KEY"

    /**
     * Keys for challenge duration hours, minutes, and seconds.
     */
    const val key_challengeDurationHH = "CHALLENGEDURATIONHH_KEY"
    const val key_challengeDurationMM = "CHALLENGEDURATIONMM_KEY"
    const val key_challengeDurationSS = "CHALLENGEDURATIONSS_KEY"

    /**
     * Key indicating whether the challenge is based on distance.
     */
    const val key_modeChallengeDistance = "MODECHALLENGEDISTANCE_KEY"

    /**
     * Key for the total distance of the challenge.
     */
    const val key_challengeDistance = "CHALLENGEDISTANCE_KEY"

    /**
     * Key indicating whether to show notifications during the challenge.
     */
    const val key_challengeNotify = "NOTIFYCHALLENGE_KEY"

    /**
     * Key indicating whether the challenge should finish automatically when conditions are met.
     */
    const val key_challengeAutoFinish = "AUTOFINISH_KEY"
}