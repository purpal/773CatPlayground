package com.purpal.catplayground.nova

import kotlin.math.*

/** Unit sphere geometry shared by the flight animation and its tests. */
object GlobeMath {
    data class Vec(val x: Double, val y: Double, val z: Double) {
        fun dot(b: Vec) = x*b.x + y*b.y + z*b.z
        fun unit(): Vec { val n = sqrt(dot(this)); return Vec(x/n, y/n, z/n) }
    }
    data class Projection(val x: Float, val y: Float, val depth: Float)
    fun point(lon: Double, lat: Double): Vec {
        val a = Math.toRadians(lon); val b = Math.toRadians(lat)
        return Vec(cos(b)*sin(a), sin(b), cos(b)*cos(a))
    }
    fun route(a: Vec, b: Vec, progress: Double): Vec {
        val t = progress.coerceIn(0.0, 1.0)
        val dot = a.dot(b).coerceIn(-1.0, 1.0)
        if (dot > 0.9999) return Vec(a.x*(1-t)+b.x*t, a.y*(1-t)+b.y*t, a.z*(1-t)+b.z*t).unit()
        if (dot < -0.9999) {
            val axis = if (abs(a.y) < 0.9) Vec(-a.z, 0.0, a.x).unit() else Vec(0.0, -a.z, a.y).unit()
            return Vec(a.x*cos(PI*t)+axis.x*sin(PI*t), a.y*cos(PI*t)+axis.y*sin(PI*t), a.z*cos(PI*t)+axis.z*sin(PI*t)).unit()
        }
        val theta = acos(dot); val u = sin((1-t)*theta)/sin(theta); val v = sin(t*theta)/sin(theta)
        return Vec(a.x*u+b.x*v, a.y*u+b.y*v, a.z*u+b.z*v).unit()
    }
    fun project(p: Vec, camera: Vec): Projection {
        val lon = atan2(camera.x, camera.z); val lat = asin(camera.y.coerceIn(-1.0,1.0))
        val right = Vec(cos(lon), 0.0, -sin(lon))
        val up = Vec(-sin(lat)*sin(lon),cos(lat),-sin(lat)*cos(lon))
        return Projection(p.dot(right).toFloat(), -p.dot(up).toFloat(), p.dot(camera).toFloat())
    }
}
