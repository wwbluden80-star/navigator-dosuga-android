import ru.navigatordosuga.app.game.tower.*
import kotlin.math.*

fun main(){
    // Rope release must preserve accumulated horizontal velocity rather than zeroing it.
    val sim=TowerSimulation(seed=7)
    repeat(180){sim.step(1.0/60.0)}
    val before=sim.current!!.vx
    sim.release()
    val immediate=sim.current!!.vx
    check(abs(before-immediate)<1e-9){"release changed velocity: $before -> $immediate"}
    repeat(60){sim.step(1.0/60.0)}
    check(sim.current!!.y.isFinite())

    check(TowerScoring.classify(.98,.4)==PlacementQuality.PERFECT)
    check(TowerScoring.classify(.90,2.0)==PlacementQuality.GREAT)
    check(TowerScoring.classify(.35,8.0)==PlacementQuality.RISKY)
    var score=ScoreState();repeat(6){score=TowerScoring.apply(score,PlacementQuality.PERFECT)}
    check(score.floors==6 && score.bestCombo==6 && score.perfects==6)

    // Numerical stress: 200 bodies resting on a static platform, stepped for 20s.
    val w=TowerWorld();w.addBody(TowerBody("platform",195.0,830.0,250.0,24.0,static=true,friction=.96))
    val bodies=(0 until 200).map{i->w.addBody(TowerBody("b$i",195.0,790.0-i*74.02,180.0,74.0,friction=.84,restitution=.01))}
    repeat(1200){w.step(1.0/60.0)}
    check(bodies.all{it.x.isFinite()&&it.y.isFinite()&&it.angle.isFinite()})
    val maxDrift=bodies.maxOf{abs(it.x-195.0)}
    println("TOWER_JVM_PASS releaseV=$immediate score=${score.score} floors=${score.floors} maxDrift=$maxDrift")
}
