package com.posepilot.app.pose.matching

import com.posepilot.app.models.BodyRegion
import com.posepilot.app.pose.analysis.PoseFeature
import com.posepilot.app.pose.rules.RuleEngine
import com.posepilot.app.pose.rules.RuleResult
import com.posepilot.app.pose.rules.RuleStatus
import com.posepilot.app.targetpose.PoseTemplate

data class RegionScore(val region: BodyRegion, val score: Double, val known: Boolean)

data class PoseMatch(
    val results: List<RuleResult>,
    val regionScores: List<RegionScore>,
    /** 0..1 weighted over regions that could be measured. */
    val overall: Double,
    /** Fraction of the template's rule weight that could actually be measured this frame. */
    val coverage: Double,
) {
    val offRules: List<RuleResult> get() = results.filter { it.status == RuleStatus.OFF }
    val allWithinTolerance: Boolean get() = results.none { it.status == RuleStatus.OFF }
}

/**
 * Weighted pose score: inside each body region the weighted mean rule score is blended 50/50 with the worst
 * rule; regions are combined with [BodyRegion.weight] and the total is scaled by (0.6 + 0.4 * worst region). Regions with no measurable rule are left out and the rest re-weighted,
 * but [coverage] records how much was missing so the UI never shows 100% for a half-visible person.
 */
object PoseMatcher {

    fun match(template: PoseTemplate, features: Map<PoseFeature, Double>): PoseMatch {
        val results = RuleEngine.evaluate(template.rules, features)
        return fromResults(results)
    }

    fun fromResults(results: List<RuleResult>): PoseMatch {
        val totalWeight = results.sumOf { it.rule.weight }
        val knownWeight = results.filter { it.status != RuleStatus.UNKNOWN }.sumOf { it.rule.weight }
        val coverage = if (totalWeight > 0) knownWeight / totalWeight else 0.0

        val regionScores = BodyRegion.entries.mapNotNull { region ->
            val inRegion = results.filter { it.rule.feature.region == region }
            if (inRegion.isEmpty()) return@mapNotNull null
            val known = inRegion.filter { it.status != RuleStatus.UNKNOWN }
            val w = known.sumOf { it.rule.weight }
            if (known.isEmpty() || w <= 0) RegionScore(region, 0.0, known = false)
            else {
                // Blend mean with the worst rule so one badly wrong joint can't hide behind two good ones.
                val mean = known.sumOf { it.score * it.rule.weight } / w
                val worst = known.minOf { it.score }
                RegionScore(region, 0.5 * mean + 0.5 * worst, known = true)
            }
        }
        val knownRegions = regionScores.filter { it.known }
        val regionWeight = knownRegions.sumOf { it.region.weight }
        val raw = if (regionWeight > 0) knownRegions.sumOf { it.score * it.region.weight } / regionWeight else 0.0
        // The weakest region scales the total: an arm that is completely wrong must visibly lower the match.
        val worstRegion = knownRegions.minOfOrNull { it.score } ?: 0.0
        // Unmeasured parts pull the score down proportionally: we can't claim a match we couldn't see.
        val overall = raw * (0.6 + 0.4 * worstRegion) * (0.4 + 0.6 * coverage)
        return PoseMatch(results, regionScores, overall.coerceIn(0.0, 1.0), coverage)
    }
}
