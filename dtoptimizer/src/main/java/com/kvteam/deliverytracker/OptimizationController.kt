package com.kvteam.deliverytracker
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@Suppress("unused")
@RestController
class OptimizationController {

    @Value("\${try_keep_performers:false}")
    var tryKeepPerformers: String = "false"

    @GetMapping("/")
    fun abc () : String{
        return tryKeepPerformers
    }

    @PostMapping("/")
    fun optimize(@RequestBody request: OptimizationRequest): OptimizationResponse {
        val requestTasks = request.tasks
        val performers = request.performers
        val weights = request.weights
        if (requestTasks == null
            || performers == null
            || weights == null) {
            return OptimizationResponse(null)
        }
        val tasks = mutableListOf<TaskGene>()
        for(requestTask in requestTasks) {
            if (requestTask.taskId == null
                || requestTask.startTimeOffset == null
                || requestTask.endTimeOffset == null) {
                return OptimizationResponse(null)
            }
            tasks.add(TaskGene(requestTask.taskId!!, requestTask.performerId, requestTask.startTimeOffset!!, requestTask.endTimeOffset!!))
        }
        val keepPerformers = tryKeepPerformers == "true"
        val problem = createProblem(tasks, performers, weights, keepPerformers)

        var iterations = 2500
        var populationSize = 200
        var cnt = 0
        var route: List<Route>? = null
        while (cnt < 3) {
            cnt++
            val result = runEMOEA(problem)
            route = processResult(result, tasks, performers, weights, keepPerformers)
            if (route == null) {
                iterations *= 2
                populationSize += 50
            } else {
                cnt = 3
            }
        }

        return OptimizationResponse(route)
    }

}