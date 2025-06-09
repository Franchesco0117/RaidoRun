package com.francisco.raidorun

/**
 * Runs
 *
 * Data class representing a single sport activity record.
 * Stores metadata about the run including date, distance, speed,
 * interval/challenge mode information, GPS coordinates, and medals earned.
 *
 * Used to populate the RecyclerView in RecordActivity.
 *
 * Author: Francisco Castro
 * Created: 19/MAY/2025
 */
data class Runs(

    /** Date of the run (e.g., "2025-06-07") */
    var date: String? = null,

    /** Start time of the run (e.g., "08:30 AM") */
    var startTime: String? = null,

    /** Email or ID of the user who performed the run */
    var user: String? = null,

    /** Duration of the run in HH:mm:ss format */
    var duration: String? = null,

    /** Indicates if the run used interval mode (true = intervals enabled) */
    var intervalMode: Boolean? = null,

    /** Interval duration in seconds, if intervalMode is true */
    var intervalDuration: Int? = null,

    /** Total running time in HH:mm:ss format during intervals */
    var runningTime: String? = null,

    /** Total walking time in HH:mm:ss format during intervals */
    var walkingTime: String? = null,

    /** Target duration defined as a challenge goal (HH:mm:ss) */
    var challengeDuration: String? = null,

    /** Target distance defined as a challenge goal (in kilometers/meters) */
    var challengeDistance: Double? = null,

    /** Total distance covered in the run (in kilometers/meters) */
    var distance: Double? = null,

    /** Maximum speed reached during the run (in km/h or m/s) */
    var maxSpeed: Double? = null,

    /** Average speed over the course of the run (in km/h or m/s) */
    var avgSpeed: Double? = null,

    /** Minimum altitude recorded during the run (in meters) */
    var minAltitude: Double? = null,

    /** Maximum altitude recorded during the run (in meters) */
    var maxAltitude: Double? = null,

    /** Minimum latitude recorded during the run (for bounding box) */
    var minLatitude: Double? = null,

    /** Maximum latitude recorded during the run (for bounding box) */
    var maxLatitude: Double? = null,

    /** Minimum longitude recorded during the run (for bounding box) */
    var minLongitude: Double? = null,

    /** Maximum longitude recorded during the run (for bounding box) */
    var maxLongitude: Double? = null,

    /** Central latitude of the run route (for map focus) */
    var centerLatitude: Double? = null,

    /** Central longitude of the run route (for map focus) */
    var centerLongitude: Double? = null,

    /** Type of sport performed: "Running", "Bike", or "RollerSkate" */
    var sport: String? = null,

    /** Indicates if GPS was active during the run */
    var activatedGPS: Boolean? = null,

    /** Medal level earned for distance (e.g., "Gold", "Silver") */
    var medalsDistance: String? = null,

    /** Medal level earned for average speed */
    var medalsAvgSpeed: String? = null,

    /** Medal level earned for maximum speed */
    var medalsMaxSpeed: String? = null
)
