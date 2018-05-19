package com.kvteam.deliverytracker

import java.util.*

data class TaskGene (
        var taskId: UUID,
        var performerId: UUID?,
        var startTimeOffset: Int,
        var endTimeOffset: Int)