package com.kvteam.deliverytracker

import java.util.*


class OptimizationRequest {
    var tasks: List<TaskRouteItem>? = null
    var performers: List<UUID>? = null
    var weights: Array<Array<Int>>? = null
}