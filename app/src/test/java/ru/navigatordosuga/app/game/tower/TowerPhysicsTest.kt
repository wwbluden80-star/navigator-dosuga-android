package ru.navigatordosuga.app.game.tower

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class TowerPhysicsTest {
 @Test fun releasePreservesMomentum(){val s=TowerSimulation(seed=7);repeat(180){s.step(1.0/60)};val v=s.current!!.vx;s.release();assertEquals(v,s.current!!.vx,1e-9)}
 @Test fun scoringMatchesWebContract(){var s=ScoreState();repeat(6){s=TowerScoring.apply(s,PlacementQuality.PERFECT)};assertEquals(6,s.floors);assertEquals(1467,s.score);assertEquals(6,s.bestCombo)}
 @Test fun twoHundredBodiesStayFinite(){val w=TowerWorld();w.addBody(TowerBody("platform",195.0,830.0,250.0,24.0,static=true));val b=(0 until 200).map{i->w.addBody(TowerBody("$i",195.0,790.0-i*74.02,180.0,74.0,restitution=.01))};repeat(1200){w.step(1.0/60)};assertTrue(b.all{it.x.isFinite()&&it.y.isFinite()&&it.angle.isFinite()});assertTrue(b.maxOf{abs(it.x-195.0)}<1.0)}
}
