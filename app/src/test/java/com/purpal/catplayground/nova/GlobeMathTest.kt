package com.purpal.catplayground.nova

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GlobeMathTest {
    @Test fun destinationAndDepartureArePreservedAcrossDateLine() {
        val a=GlobeMath.point(170.0,20.0);val b=GlobeMath.point(-170.0,20.0)
        assertTrue(GlobeMath.route(a,b,0.0).dot(a)>.99999)
        assertTrue(GlobeMath.route(a,b,1.0).dot(b)>.99999)
        val middle=GlobeMath.route(a,b,.5)
        assertTrue(middle.z<-.9) // Short route crosses the Pacific, not Greenwich.
        assertEquals(1.0,middle.dot(middle),1e-6)
    }
    @Test fun antipodalAndIdenticalDestinationsDoNotProduceNaN() {
        val a=GlobeMath.point(0.0,0.0)
        for(b in listOf(a,GlobeMath.point(180.0,0.0))) {
            for(i in 0..20) {
                val p=GlobeMath.route(a,b,i/20.0)
                assertTrue(p.x.isFinite() && p.y.isFinite() && p.z.isFinite())
                assertEquals(1.0,p.dot(p),1e-6)
            }
        }
    }
    @Test fun projectionHidesBackSideAndCentersCameraLocation() {
        val camera=GlobeMath.point(121.0,23.7)
        val front=GlobeMath.project(camera,camera)
        val back=GlobeMath.project(GlobeMath.Vec(-camera.x,-camera.y,-camera.z),camera)
        assertEquals(0f,front.x,1e-5f);assertEquals(0f,front.y,1e-5f)
        assertTrue(front.depth>.99);assertTrue(back.depth<-.99)
    }
}
