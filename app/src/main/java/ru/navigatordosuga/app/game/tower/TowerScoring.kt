package ru.navigatordosuga.app.game.tower

data class ScoreState(val floors:Int=0,val score:Int=0,val combo:Int=0,val perfects:Int=0,val bestCombo:Int=0,val placements:String="")
enum class PlacementQuality{PERFECT,GREAT,RISKY}
object TowerScoring{
    fun apply(s:ScoreState,q:PlacementQuality):ScoreState{val floor=s.floors+1;return when(q){PlacementQuality.PERFECT->{val combo=s.combo+1;val pts=100+kotlin.math.min(5,combo-1)*55+floor*2;s.copy(floors=floor,score=s.score+pts,combo=combo,perfects=s.perfects+1,bestCombo=kotlin.math.max(s.bestCombo,combo),placements=s.placements+"P")};PlacementQuality.GREAT->s.copy(floors=floor,score=s.score+120+floor,combo=0,placements=s.placements+"G");PlacementQuality.RISKY->s.copy(floors=floor,score=s.score+100+floor,combo=0,placements=s.placements+"R")}}
    fun classify(overlap:Double,relativeAngleDeg:Double)=when{overlap>=.96&&kotlin.math.abs(relativeAngleDeg)<=1.2->PlacementQuality.PERFECT;overlap>=.88&&kotlin.math.abs(relativeAngleDeg)<=4.5->PlacementQuality.GREAT;else->PlacementQuality.RISKY}
}
