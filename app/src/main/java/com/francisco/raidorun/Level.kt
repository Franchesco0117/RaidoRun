package com.francisco.raidorun

/**
 * Level
 *
 * Represents a specific level or stage within the application, typically used to
 * set exercise goals for users such as number of runs or total distance.
 *
 * Properties:
 * - name: Optional name of the level.
 * - image: Optional image URL or identifier representing the level.
 * - runsTarget: Optional number of runs required to complete the level.
 * - distanceTarget: Optional distance (in meters, kilometers, etc.) required to complete the level.
 *
 * This class is likely used for gamification or progress tracking within the app.
 *
 * Author: [Francisco Castro]
 * Created: [17/MAY/2025]
 */
data class Level(
    var name: String ?= null,
    var image: String ?= null,
    var runsTarget: Int ?= null,
    var distanceTarget: Int ?= null
) {

}
