package com.kvteam.deliverytracker

import java.util.*

data class Route (
        var taskRoute: MutableList<Int>,
        var eta: MutableList<Int>,
        var performerId: UUID?
)
