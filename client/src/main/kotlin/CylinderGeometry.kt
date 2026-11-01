import kotlin.math.*

class CylinderGeometry(
  gl : WebGL2RenderingContext,
  segments : Int = 32,
  sideTexHeight : Float = 1f) : IndexedGeometry(gl, cylinder(segments, sideTexHeight)) {

  companion object {
    private fun cylinder(segments : Int, sideTexHeight : Float) : GeometryData {
      val data = GeometryData()
      val angles = (0..segments).map { it.toFloat() / segments * 2f * PI.toFloat() }

      for ((i, phi) in angles.withIndex()) {
        for (y in listOf(0f, 1f)) {
          data.vertex(cos(phi), y, sin(phi), cos(phi), 0f, sin(phi), i.toFloat() / segments, (1f - y) * sideTexHeight)
        }
      }
      for (i in 0 until segments) {
        val b = 2 * i
        data.triangle(b, b + 1, b + 2)
        data.triangle(b + 1, b + 3, b + 2)
      }

      val center = data.vertex(0f, 1f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
      for (phi in angles) {
        data.vertex(cos(phi), 1f, sin(phi), 0f, 1f, 0f, 0.5f + 0.5f * cos(phi), 0.5f + 0.5f * sin(phi))
      }
      for (i in 0 until segments) {
        data.triangle(center, center + 1 + i, center + 2 + i)
      }
      return data
    }
  }
}
