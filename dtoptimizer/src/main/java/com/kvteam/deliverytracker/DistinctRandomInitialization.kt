package com.kvteam.deliverytracker

import org.moeaframework.core.Initialization
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.RealVariable

class DistinctRandomInitialization(
        private val problem: Problem,
        private val populationSize: Int): Initialization {
    override fun initialize(): Array<Solution> {
        val initialPopulation = mutableListOf<Solution>()

        for (i in 0 until this.populationSize) {
            val solution = this.problem.newSolution()

            val list = MutableList(solution.numberOfVariables, { it })
            list.shuffle()

            for (j in 0 until solution.numberOfVariables) {
                solution.setVariable(j, RealVariable(list[j].toDouble(), 0.0, solution.numberOfVariables - 0.001))
            }

            initialPopulation.add(solution)
        }

        return initialPopulation.toTypedArray()
    }

}