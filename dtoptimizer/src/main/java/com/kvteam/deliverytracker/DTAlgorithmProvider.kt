package com.kvteam.deliverytracker

import org.moeaframework.algorithm.EpsilonMOEA
import org.moeaframework.algorithm.MOEAD
import org.moeaframework.algorithm.NSGAII
import org.moeaframework.algorithm.single.AggregateObjectiveComparator
import org.moeaframework.algorithm.single.GeneticAlgorithm
import org.moeaframework.algorithm.single.LinearDominanceComparator
import org.moeaframework.algorithm.single.MinMaxDominanceComparator
import org.moeaframework.analysis.sensitivity.EpsilonHelper
import org.moeaframework.core.*
import org.moeaframework.core.comparator.ChainedComparator
import org.moeaframework.core.comparator.CrowdingComparator
import org.moeaframework.core.comparator.DominanceComparator
import org.moeaframework.core.comparator.ParetoDominanceComparator
import org.moeaframework.core.operator.TournamentSelection
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.core.spi.OperatorFactory
import org.moeaframework.util.TypedProperties
import java.util.*

class DTAlgorithmProvider : AlgorithmProvider() {
    override fun getAlgorithm(name: String, props: Properties, problem: Problem): Algorithm {
        val properties = TypedProperties(props)

        if (name == "MOEA/D") {
            return getMOEAD( properties, problem)
        } else if (name == "eMOEA") {
            return geteMOEA(properties, problem)
        } else if (name == "NSGAII") {
            return getNSGAII(properties, problem)
        } else if (name == "GeneticAlgorithm") {
            return getGeneticAlgorithm(properties, problem)
        }
        throw NotImplementedError()
    }

    private fun getMOEAD(properties: TypedProperties, problem: Problem): Algorithm {

        var populationSize = properties.getDouble("populationSize", 100.0).toInt()
        if (populationSize < problem.numberOfObjectives) {
            System.err.println("increasing MOEA/D population size")
            populationSize = problem.numberOfObjectives
        }

        var neighborhoodSize = if (properties.contains("neighborhoodSize")) {
            Math.max(2, (properties.getDouble("neighborhoodSize", 0.1) * populationSize.toDouble()).toInt())
        } else {
            20
        }
        neighborhoodSize = Math.min(neighborhoodSize, populationSize)

        val eta = if (properties.contains("eta")) {
            Math.max(2, (properties.getDouble("eta", 0.01) * populationSize.toDouble()).toInt())
        } else {
            2
        }

        val initialization = DistinctRandomInitialization(problem, populationSize)
        // pcx+2x+swap
        // pmx+swap
        val variation = OperatorFactory.getInstance().getVariation("pmx+swap", properties, problem)

        return MOEAD(
                problem,
                neighborhoodSize,
                null,
                initialization,
                variation,
                properties.getDouble("delta", 0.9),
                eta.toDouble(),
                properties.getDouble("updateUtility", -1.0).toInt())
    }

    private fun geteMOEA(properties: TypedProperties, problem: Problem): Algorithm {
        val populationSize = properties.getDouble("populationSize", 100.0).toInt()
        val initialization = DistinctRandomInitialization(problem, populationSize)
        val population = Population()
        val comparator = ParetoDominanceComparator()
        val archive = EpsilonBoxDominanceArchive(properties.getDoubleArray("epsilon", doubleArrayOf(EpsilonHelper.getEpsilon(problem))))
        val selection = TournamentSelection(2, comparator)
        val variation = OperatorFactory.getInstance().getVariation(null as String?, properties, problem)
        return EpsilonMOEA(problem, population, archive, selection, variation, initialization, comparator)
    }

    private fun getNSGAII(properties: TypedProperties, problem: Problem): Algorithm {
        val populationSize = properties.getDouble("populationSize", 100.0).toInt()
        val initialization = DistinctRandomInitialization(problem, populationSize)
        val population = NondominatedSortingPopulation()
        var selection: TournamentSelection? = null
        if (properties.getBoolean("withReplacement", true)) {
            selection = TournamentSelection(2, ChainedComparator(*arrayOf(ParetoDominanceComparator(), CrowdingComparator())))
        }

        val variation = OperatorFactory.getInstance().getVariation(null as String?, properties, problem)
        return NSGAII(problem, population, null as EpsilonBoxDominanceArchive?, selection, variation, initialization)
    }

    private fun getGeneticAlgorithm(properties: TypedProperties, problem: Problem): Algorithm {
        val populationSize = properties.getDouble("populationSize", 100.0).toInt()
        val weights = properties.getDoubleArray("weights", doubleArrayOf(1.0))
        val method = properties.getString("method", "linear")
        var comparator: AggregateObjectiveComparator? = null
        if (method.equals("linear", ignoreCase = true)) {
            comparator = LinearDominanceComparator(*weights)
        } else {
            if (!method.equals("min-max", ignoreCase = true)) {
                throw FrameworkException("unrecognized weighting method: $method")
            }

            comparator = MinMaxDominanceComparator(*weights)
        }

        val initialization = DistinctRandomInitialization(problem, populationSize)
        val selection = TournamentSelection(2, comparator as DominanceComparator?)
        val variation = OperatorFactory.getInstance().getVariation(null as String?, properties, problem)
        return GeneticAlgorithm(problem, comparator as AggregateObjectiveComparator?, initialization, selection, variation)
    }

}