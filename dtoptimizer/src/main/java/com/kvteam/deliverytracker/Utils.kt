package com.kvteam.deliverytracker

import org.moeaframework.Executor
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.EncodingUtils
import java.util.*


private const val MaxValue = Int.MAX_VALUE.toDouble() - 1

fun processResult (result: NondominatedPopulation, tasks: List<TaskGene>, performers: List<UUID>, weight: Array<Array<Int>>) : List<Route>? {
    var bestSolution: Solution? = null

    for (solution in result) {
        if (solution.getObjective(1) < bestSolution?.getObjective(1) ?: MaxValue) {
            bestSolution = solution
        }
    }

    if (bestSolution == null
        || bestSolution.objectives.any { it >= MaxValue }) {
        return null
    }

    val chromosome = EncodingUtils.getInt(bestSolution)
    return buildRoutes(chromosome, tasks, performers, weight)
}

fun createProblem (tasks: List<TaskGene>, performers: List<UUID>, weight: Array<Array<Int>>) : VRPProblem {

    return VRPProblem(performers, tasks, weight)
}

fun runEMOEA (problem: VRPProblem, iterations: Int = 1000, population: Int = 200): NondominatedPopulation {
    val executor = Executor()
            .withProblem(problem)
            .withAlgorithm("eMOEA")
            .withMaxEvaluations(iterations)

            .withProperty("populationSize", population)
            .distributeOnAllCores()

    return executor.run()
}

fun runMOEAD (problem: VRPProblem, iterations: Int = 1000, population: Int = 200) : NondominatedPopulation {
    val executor = Executor()
            .withProblem(problem)
            .withAlgorithm("MOEA/D")
            .withMaxEvaluations(iterations)

            .withProperty("populationSize", population)
            // the probability of mating with a solution in the neighborhood rather than the entire population
            .withProperty("delta", 0.9)
            // the maximum number of population slots a solution can replace
            .withProperty("eta", 2)
            // the frequency, in generations, in which utility values are updated; set to 50 to use the recommended update frequency or -1 to disable utility-based search.
            .withProperty("updateUtility", -1)

            // pcx
            .withProperty("pcx.parents", 5.0)
            .withProperty("pcx.offspring", 2)
            .withProperty("pcx.eta", 0.1)
            .withProperty("pcx.zeta", 0.1)
            // Коэффициент two-point crossover
            .withProperty("2x.rate", 0.7)
            // Коэффициент мутации
            .withProperty("swap.rate", 0.075)

            .distributeOnAllCores()
    return executor.run()
}

fun buildRoutes (chromosome: IntArray, tasks: List<TaskGene>, performers: List<UUID>, weight: Array<Array<Int>>) : List<Route> {
    val routes = mutableListOf<Route>()
    for (gene in chromosome) {
        var added = false
        for (route in routes) {
            val newEta = route.eta.last() + weight[route.taskRoute.last()][gene]
            if (tasks[gene].startTimeOffset <= newEta && newEta <= tasks[gene].endTimeOffset) {
                route.taskRoute.add(gene)
                route.eta.add(newEta)
                added = true
                break
            }
        }

        if (!added) {
            for (route in routes) {
                val newEta = route.eta.last() + weight[route.taskRoute.last()][gene]
                if (newEta < tasks[gene].startTimeOffset) {
                    route.taskRoute.add(gene)
                    route.eta.add(tasks[gene].startTimeOffset)
                    added = true
                    break
                }
            }
        }

        if (!added) {
            val newRoute = Route(mutableListOf(), mutableListOf(), performers[routes.size])
            newRoute.taskRoute.add(gene)
            newRoute.eta.add(tasks[gene].startTimeOffset)
            routes.add(newRoute)
        }
    }

    return routes
}