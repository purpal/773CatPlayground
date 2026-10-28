package com.purpal.catplayground.nova

object NovaBossArt {
    /** The 35% disarm occurs during mood 1; mood alone cannot select the right body. */
    fun body(mood: Int, broken: Boolean) = when {
        mood == 2 -> "BODY_CRITICAL"
        broken -> "BODY_DISARMED"
        mood == 1 -> "BODY_ALERT"
        else -> "BODY_HEALTHY"
    }
}
