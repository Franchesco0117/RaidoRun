package com.francisco.raidorun

/**
 * Totals
 *
 * Data class representing aggregated statistics related to runs.
 * Contains records and totals for speed, distance, runs count, and time.
 *
 * Properties:
 * - recordAvgSpeed: Highest average speed recorded.
 * - recordDistance: Longest distance recorded.
 * - recordSpeed: Maximum speed recorded.
 * - totalDistance: Total distance covered in all runs.
 * - totalRuns: Total number of runs.
 * - totalTime: Total time spent running (in minutes or seconds as defined).
 *
 * Author: Francisco Castro
 * Created: 17/MAY/2025
 */
data class Totals(
    var recordAvgSpeed: Double ?= null,
    var recordDistance: Double ?= null,
    var recordSpeed: Double ?= null,
    var totalDistance: Double ?= null,
    var totalRuns: Int ?= null,
    var totalTime: Int ?= null
) {

}
