package com.kvteam.deliverytracker

import org.moeaframework.Executor
import org.moeaframework.algorithm.StandardAlgorithms
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.EncodingUtils
import java.util.*


private const val MaxValue = Int.MAX_VALUE.toDouble() - 1

fun processResult (result: NondominatedPopulation,
                   tasks: List<TaskGene>,
                   performers: List<UUID>,
                   weight: Array<Array<Int>>,
                   keepPerformers: Boolean) : List<Route>? {
    var bestSolution: Solution? = null

    for (solution in result) {
        if (solution.getObjective(2) < bestSolution?.getObjective(2) ?: MaxValue) {
            bestSolution = solution
        }
    }

    if (bestSolution == null
        || bestSolution.objectives.any { it >= MaxValue }) {
        return null
    }

    val chromosome = EncodingUtils.getInt(bestSolution)
    return buildRoutes(chromosome, tasks, performers, weight, keepPerformers)
}

fun createProblem (tasks: List<TaskGene>, performers: List<UUID>, weight: Array<Array<Int>>, keepPerformers: Boolean) : VRPProblem {

    return VRPProblem(performers, tasks, weight, keepPerformers)
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

fun buildRoutes (chromosome: IntArray,
                 tasks: List<TaskGene>,
                 performers: List<UUID>,
                 weight: Array<Array<Int>>,
                 keepPerformers: Boolean) : List<Route> {
    val routes = mutableListOf<Route>()

    // Каждый ген в хромосоме - номер в массиве заданий.
    // Пытаемся разбить одну последовательность на несколько подпоследовательностей (маршрутов)
    for (gene in chromosome) {
        var added = false

        // Если у задания уже назначен исполнитель,
        // то пытаемся его сохранить
        if (keepPerformers && tasks[gene].performerId != null) {
            val perfIdx = performers.indexOf(tasks[gene].performerId)
            // Маршрут на исполнителя еще даже не создан
            if (routes.size <= perfIdx) {
                // создадим его ( и все до него, оставив их пустыми )
                while (routes.size <= perfIdx) {
                    routes.add(Route(mutableListOf(), mutableListOf(), performers[perfIdx]))
                }
                // Положим первым таском
                val route = routes.last()
                route.taskRoute.add(gene)
                route.eta.add(tasks[gene].startTimeOffset)
                added = true
            } else {
                val route = routes[perfIdx]
                // Маршрут на исполнителя создан, но пустой
                if (route.taskRoute.isEmpty()) {
                    route.taskRoute.add(gene)
                    route.eta.add(tasks[gene].startTimeOffset)
                    added = true
                } else {
                    // Маршрут не пустой, пытаемся добавить туда задание, если укладываемся по времени.
                    val newEta = route.eta.last() + weight[route.taskRoute.last()][gene]
                    if (tasks[gene].startTimeOffset <= newEta && newEta <= tasks[gene].endTimeOffset) {
                        route.taskRoute.add(gene)
                        route.eta.add(newEta)
                        added = true
                    }
                }
            }
        }

        if (!added) {
            // Ищем подходящий маршрут для задания, чтобы доставить в окно
            for (route in routes) {
                if (route.taskRoute.isEmpty()) {
                    route.taskRoute.add(gene)
                    route.eta.add(tasks[gene].startTimeOffset)
                    added = true
                    break
                }

                val newEta = route.eta.last() + weight[route.taskRoute.last()][gene]
                if (tasks[gene].startTimeOffset <= newEta && newEta <= tasks[gene].endTimeOffset) {
                    route.taskRoute.add(gene)
                    route.eta.add(newEta)
                    added = true
                    break
                }
            }
        }

        if (!added) {
            // Ищем подходящий маршрут, в котором придется прийти пораньше и подождать начала окна
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
            // Создаем новый маршрут
            val performer = if (routes.size < performers.size) {
                performers[routes.size]
            } else {
                UUID(0,0)
            }

            val newRoute = Route(mutableListOf(), mutableListOf(), performer)
            newRoute.taskRoute.add(gene)
            newRoute.eta.add(tasks[gene].startTimeOffset)
            routes.add(newRoute)
        }
    }

    return routes
}