package ru.navigatordosuga.app.game.tower

import kotlin.math.*

data class V2(val x:Double,val y:Double){operator fun plus(o:V2)=V2(x+o.x,y+o.y);operator fun minus(o:V2)=V2(x-o.x,y-o.y);operator fun times(s:Double)=V2(x*s,y*s)}
private fun dot(a:V2,b:V2)=a.x*b.x+a.y*b.y
private fun cross(a:V2,b:V2)=a.x*b.y-a.y*b.x
private fun len(a:V2)=hypot(a.x,a.y).coerceAtLeast(1e-9)
private fun norm(a:V2):V2{val l=len(a);return V2(a.x/l,a.y/l)}
private fun rot(a:V2,r:Double)=V2(a.x*cos(r)-a.y*sin(r),a.x*sin(r)+a.y*cos(r))

class TowerBody(
    val id:String,
    var x:Double,var y:Double,val w:Double,val h:Double,var angle:Double=0.0,
    var vx:Double=0.0,var vy:Double=0.0,var av:Double=0.0,
    val static:Boolean=false,density:Double=.003,
    val friction:Double=.78,val restitution:Double=.025,
    val linearDamping:Double=.006,val angularDamping:Double=.018
){
    val mass=if(static)Double.POSITIVE_INFINITY else max(1.0,w*h*density)
    val invM=if(static)0.0 else 1.0/mass
    val inertia=if(static)Double.POSITIVE_INFINITY else mass*(w*w+h*h)/12.0
    val invI=if(static)0.0 else 1.0/inertia
    var sleeping=false;var sleepTime=0.0;var sleepLocked=false
    var settledX:Double?=null;var settledY:Double?=null;var settledAngle:Double?=null
    fun vertices():List<V2>{val hw=w/2;val hh=h/2;return listOf(V2(-hw,-hh),V2(hw,-hh),V2(hw,hh),V2(-hw,hh)).map{worldPoint(it)}}
    fun worldPoint(local:V2)=V2(x,y)+rot(local,angle)
    fun velocityAt(p:V2):V2{val rx=p.x-x;val ry=p.y-y;return V2(vx-av*ry,vy+av*rx)}
    fun applyImpulse(j:V2,p:V2){if(static||sleepLocked)return;if(sleeping)wake();vx+=j.x*invM;vy+=j.y*invM;val r=V2(p.x-x,p.y-y);av+=cross(r,j)*invI}
    fun wake(force:Boolean=false){if(sleepLocked&&!force)return;sleeping=false;sleepTime=0.0}
}

data class Collision(val a:TowerBody,val b:TowerBody,val n:V2,val penetration:Double,val contact:V2)

class RopeConstraint(val body:TowerBody,val local:V2,val anchor:V2,var length:Double,val stiffness:Double=.92,val damping:Double=.18){var active=true;var anchorX=anchor.x;var anchorY=anchor.y
    fun solve(dt:Double){if(!active||body.static)return;val p=body.worldPoint(local);val d=V2(p.x-anchorX,p.y-anchorY);val dist=len(d);if(dist<=length*.985)return;val n=d*(1.0/dist);val err=dist-length;val r=V2(p.x-body.x,p.y-body.y);val rn=cross(r,n);val den=body.invM+rn*rn*body.invI;if(den<1e-9)return;val corr=err*stiffness/den;body.x-=n.x*corr*body.invM;body.y-=n.y*corr*body.invM;body.angle-=rn*corr*body.invI;val vp=body.velocityAt(p);val vn=dot(vp,n);val impulse=-(vn*damping+err*min(42.0,7.0/dt))/den;body.applyImpulse(n*impulse,p)}
}

class TowerWorld(var gravity:Double=1050.0,val iterations:Int=7,val substeps:Int=2,val cell:Double=130.0){
    val bodies=mutableListOf<TowerBody>();val constraints=mutableListOf<RopeConstraint>();var time=0.0;var onCollision:((Collision)->Unit)?=null
    fun addBody(b:TowerBody)=b.also{bodies+=it};fun removeBody(b:TowerBody){bodies.remove(b);constraints.removeAll{it.body===b}}
    fun addConstraint(c:RopeConstraint)=c.also{constraints+=it};fun removeConstraint(c:RopeConstraint){c.active=false;constraints.remove(c)}
    fun step(dt:Double){val sdt=dt/substeps;repeat(substeps){time+=sdt;bodies.forEach{b->if(!b.static&&!b.sleeping){b.vy+=gravity*sdt;val ld=max(0.0,1-b.linearDamping*60*sdt);val ad=max(0.0,1-b.angularDamping*60*sdt);b.vx*=ld;b.vy*=ld;b.av*=ad;b.x+=b.vx*sdt;b.y+=b.vy*sdt;b.angle+=b.av*sdt}};repeat(5){constraints.forEach{it.solve(sdt)}};val manifolds=broadphase().mapNotNull{collide(it.first,it.second)};repeat(iterations){manifolds.forEach(::resolve)};manifolds.forEach(::positional);onCollision?.let{cb->manifolds.forEach(cb)};bodies.forEach{b->if(!b.static){val sp=hypot(b.vx,b.vy);val attached=constraints.any{it.body===b&&it.active};if(sp<3&&abs(b.av)<.02&&!attached){b.sleepTime+=sdt;if(b.sleepTime>1.5){b.sleeping=true;b.vx=0.0;b.vy=0.0;b.av=0.0}}else b.sleepTime=0.0}}}}
    private fun broadphase():List<Pair<TowerBody,TowerBody>>{val map=HashMap<String,MutableList<TowerBody>>();for(b in bodies){val vs=b.vertices();val x0=floor(vs.minOf{it.x}/cell).toInt();val x1=floor(vs.maxOf{it.x}/cell).toInt();val y0=floor(vs.minOf{it.y}/cell).toInt();val y1=floor(vs.maxOf{it.y}/cell).toInt();for(x in x0..x1)for(y in y0..y1)map.getOrPut("$x,$y"){mutableListOf()}+=b};val seen=HashSet<String>();val out=ArrayList<Pair<TowerBody,TowerBody>>();for(list in map.values)for(i in list.indices)for(j in i+1 until list.size){val a=list[i];val b=list[j];if(a.static&&b.static)continue;val k=if(a.id<b.id)"${a.id}|${b.id}" else "${b.id}|${a.id}";if(seen.add(k))out+=a to b};return out}
}

private fun axes(b:TowerBody):List<V2>{val v=b.vertices();return (0..1).map{i->val e=v[(i+1)%4]-v[i];norm(V2(-e.y,e.x))}}
private fun project(v:List<V2>,a:V2):Pair<Double,Double>{var mn=Double.POSITIVE_INFINITY;var mx=Double.NEGATIVE_INFINITY;for(p in v){val d=dot(p,a);mn=min(mn,d);mx=max(mx,d)};return mn to mx}
private fun pointIn(p:V2,b:TowerBody):Boolean{val q=rot(V2(p.x-b.x,p.y-b.y),-b.angle);return abs(q.x)<=b.w/2+.5&&abs(q.y)<=b.h/2+.5}
fun collide(a:TowerBody,b:TowerBody):Collision?{if(a.static&&b.static)return null;val av=a.vertices();val bv=b.vertices();var best=Double.POSITIVE_INFINITY;var axis:V2?=null;for(ax in axes(a)+axes(b)){val ap=project(av,ax);val bp=project(bv,ax);val ov=min(ap.second,bp.second)-max(ap.first,bp.first);if(ov<=0)return null;if(ov<best){best=ov;axis=ax}};var n=axis?:return null;if(dot(V2(b.x-a.x,b.y-a.y),n)<0)n=n*(-1.0);val pts=mutableListOf<V2>();av.filterTo(pts){pointIn(it,b)};bv.filterTo(pts){pointIn(it,a)};val c=if(pts.isEmpty())V2((a.x+b.x)/2,(a.y+b.y)/2) else V2(pts.sumOf{it.x}/pts.size,pts.sumOf{it.y}/pts.size);return Collision(a,b,n,best,c)}
private fun resolve(m:Collision){val a=m.a;val b=m.b;val n=m.n;val c=m.contact;val rv=b.velocityAt(c)-a.velocityAt(c);val velN=dot(rv,n);if(velN>0)return;if(velN<-75){if(a.sleeping&&!a.sleepLocked)a.wake();if(b.sleeping&&!b.sleepLocked)b.wake()};val aim=if(a.sleeping)0.0 else a.invM;val bim=if(b.sleeping)0.0 else b.invM;val aii=if(a.sleeping)0.0 else a.invI;val bii=if(b.sleeping)0.0 else b.invI;val ra=V2(c.x-a.x,c.y-a.y);val rb=V2(c.x-b.x,c.y-b.y);val raN=cross(ra,n);val rbN=cross(rb,n);val den=aim+bim+raN*raN*aii+rbN*rbN*bii;if(den<1e-9)return;val e=min(a.restitution,b.restitution);val j=-(1+e)*velN/den;val imp=n*j;a.applyImpulse(imp*(-1.0),c);b.applyImpulse(imp,c);val rv2=b.velocityAt(c)-a.velocityAt(c);val tr=rv2-n*dot(rv2,n);val tl=len(tr);if(tl>1e-5){val t=tr*(1/tl);val raT=cross(ra,t);val rbT=cross(rb,t);val denT=aim+bim+raT*raT*aii+rbT*rbT*bii;var jt=-dot(rv2,t)/(if(denT==0.0)1.0 else denT);val mu=sqrt(a.friction*b.friction);val maxJ=abs(j)*mu;jt=jt.coerceIn(-maxJ,maxJ);val fi=t*jt;a.applyImpulse(fi*(-1.0),c);b.applyImpulse(fi,c)}}
private fun positional(m:Collision){val aim=if(m.a.sleeping)0.0 else m.a.invM;val bim=if(m.b.sleeping)0.0 else m.b.invM;val den=aim+bim;if(den<=0)return;val mag=max(m.penetration-.08,0.0)*.58/den;val c=m.n*mag;if(!m.a.static&&!m.a.sleeping){m.a.x-=c.x*aim;m.a.y-=c.y*aim};if(!m.b.static&&!m.b.sleeping){m.b.x+=c.x*bim;m.b.y+=c.y*bim}}
