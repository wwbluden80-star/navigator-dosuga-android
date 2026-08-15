package ru.navigatordosuga.app.game.tower

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerEngineTest {
    @Test
    fun releasePreservesHorizontalVelocity() {
        val simulation = TowerSimulation(seed = 7)
        repeat(180) { simulation.step(1.0 / 60.0) }
        val before = requireNotNull(simulation.current).vx

        simulation.release()

        val after = requireNotNull(simulation.current).vx
        assertEquals(before, after, 1e-9)
        repeat(60) { simulation.step(1.0 / 60.0) }
        assertTrue(requireNotNull(simulation.current).y.isFinite())
    }

    @Test
    fun scoringClassifiesPlacementsAndTracksCombo() {
        assertEquals(PlacementQuality.PERFECT, TowerScoring.classify(.98, .4))
        assertEquals(PlacementQuality.GREAT, TowerScoring.classify(.90, 2.0))
        assertEquals(PlacementQuality.RISKY, TowerScoring.classify(.35, 8.0))

        var score = ScoreState()
        repeat(6) { score = TowerScoring.apply(score, PlacementQuality.PERFECT) }
        assertEquals(6, score.floors)
        assertEquals(6, score.bestCombo)
        assertEquals(6, score.perfects)
        assertEquals(1467, score.score)
    }

    @Test
    fun physicsRemainsFiniteUnderLoad() {
        val world = TowerWorld()
        world.addBody(TowerBody("platform", 195.0, 830.0, 250.0, 24.0, static = true, friction = .96))
        val bodies = (0 until 50).map { index ->
            world.addBody(TowerBody("b$index", 195.0, 790.0 - index * 74.02, 180.0, 74.0, friction = .84, restitution = .01))
        }

        repeat(300) { world.step(1.0 / 60.0) }

        assertTrue(bodies.all { it.x.isFinite() && it.y.isFinite() && it.angle.isFinite() })
        assertTrue(bodies.maxOf { abs(it.x - 195.0) } < 1.0)
    }
}
