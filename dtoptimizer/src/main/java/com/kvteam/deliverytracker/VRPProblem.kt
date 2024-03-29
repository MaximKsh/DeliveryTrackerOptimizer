package com.kvteam.deliverytracker

import org.moeaframework.core.Solution
import org.moeaframework.core.variable.EncodingUtils
import org.moeaframework.core.variable.RealVariable
import org.moeaframework.problem.AbstractProblem
import java.util.*


class VRPProblem (
        private val performers: List<UUID>,
        private val tasks: List<TaskGene>,
        private val weightMatrix: Array<Array<Int>>,
        private val keepPerformers: Boolean
): AbstractProblem(tasks.size, 3) {
    override fun newSolution(): Solution {
        val solution = Solution(numberOfVariables, numberOfObjectives)
        for (i in 0 until numberOfVariables) {
            solution.setVariable(i, RealVariable(0.0, numberOfVariables - 0.001))
        }
        return solution
    }

    override fun evaluate(solution: Solution?) {
        if (solution == null) {
            return
        }
        val chromosome = EncodingUtils.getInt(solution)
        val routes = divideIntoRoutes(chromosome)

        if (!check(chromosome, routes)) {
            for(i in 0 until numberOfObjectives) {
                solution.setObjective(i, Int.MAX_VALUE.toDouble())
            }
            return
        }

        val minimalAverageRoute = routes.minBy { timeDist(it) / numberOfVariables }!!
        val maxTimeRoute  = routes.maxBy { timeDist(it) }!!

        val n = routes.size +  (timeDist(minimalAverageRoute)) / numberOfVariables
        val d = routes.sumBy { timeDist(it) }
        val b = timeDist(maxTimeRoute) - d / routes.size

        solution.setObjective(0, n.toDouble())
        solution.setObjective(1, d.toDouble())
        solution.setObjective(2, b.toDouble())

    }

    private fun timeDist (route: Route) : Int {
        return route.eta.last() - route.eta.first()
    }

    private fun check (
            chromosome: IntArray,
            routes: List<Route>): Boolean {
        // Не превышено количество курьеров
        if(routes.size > performers.size) {
            return false
        }

        // каждый посещен один раз
        val set = chromosome.toHashSet()
        return set.size == chromosome.size
    }

    private fun divideIntoRoutes(chromosome: IntArray) =
            buildRoutes(chromosome, tasks, performers, weightMatrix, keepPerformers)
}