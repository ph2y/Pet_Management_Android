package com.sju18001.petmanagement.controller

/**
 * yyyy-MM-dd
 */
class FormattedDate {
    private var formattedString: String? = null
    private var year: Int? = null
    private var month: Int? = null
    private var day: Int? = null

    constructor(formattedString: String) {
        this.formattedString = formattedString

        this.year = formattedString.substring(0, 4).toInt()
        this.month = formattedString.substring(5, 7).toInt()
        this.day = formattedString.substring(8, 10).toInt()
    }

    constructor(year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month
        this.day = day

        this.formattedString = "${year}-${month.toString().padStart(2, '0')}" +
                "-${day.toString().padStart(2, '0')}"
    }

    fun getFormattedString(): String = formattedString!!
    fun getYear(): Int = year!!
    fun getMonth(): Int = month!!
    fun getDay(): Int = day!!
}