package com.example.apfinalproject.model

object InterestCategories {
    data class Interest(
        val category: String,
        var selected: Boolean,
    )

    fun getInterests(): List<Interest> {
        return eventCategories
    }

    private val eventCategories: List<Interest> =
        listOf(
            Interest("Today", false),
            Interest("This Week", false),
            Interest("This Month", false),
            Interest("Business", false),
            Interest("Health", false),
            Interest("Music", false),
            Interest("Auto, Boat & Air", false),
            Interest("Charity & Causes", false),
            Interest("Community", false),
            Interest("Family & Education", false),
            Interest("Fashion", false),
            Interest("Film & Media", false),
            Interest("Food & Drink", false),
            Interest("Government", false),
            Interest("Hobbies", false),
            Interest("Home & Lifestyle", false),
            Interest("Performing & Visual Arts", false),
            Interest("Spirituality", false),
            Interest("School Activities", false),
            Interest("Science & Tech", false),
            Interest("Holidays", false),
            Interest("Sports & Fitness", false),
            Interest("Travel & Outdoor", false),
            Interest("Other", false),
        )
}
