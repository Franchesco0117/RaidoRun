package com.francisco.raidorun

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * ValidateEmail
 *
 * Utility class containing a method to validate email addresses
 * using a regular expression pattern.
 *
 * Author: Francisco Castro
 * Created: 9/MAR/2025
 */
class ValidateEmail {
    companion object {
        var pat: Pattern ?= null
        var mat: Matcher?= null

        /**
         * Validates whether the provided string is a valid email address
         * using a regular expression pattern.
         *
         * @param email The string to be validated as an email.
         * @return True if the string matches the email pattern, false otherwise.
         */
        fun isEmail(email: String): Boolean {
            pat = Pattern.compile("^[\\w\\-\\_\\+]+(\\.[\\w\\-\\_]+)*@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,4}$")
            mat = pat!!.matcher(email)
            return mat!!.find()
        }
    }
}