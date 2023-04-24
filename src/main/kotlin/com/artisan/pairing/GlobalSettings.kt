package com.artisan.pairing

open class GlobalSettings (
    var buyInChipCount: Int = 10,
    var chipValue: Int = 10,
    var runOnLoad: Boolean = true
) {
    fun resetDefaults() {
        buyInChipCount = 10
        chipValue = 10
        runOnLoad = true
    }
}

object Settings : GlobalSettings()