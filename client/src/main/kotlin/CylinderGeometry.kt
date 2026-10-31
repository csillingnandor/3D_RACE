import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import vision.gears.webglmath.Geometry
import kotlin.math.*

// Upright cylinder with radius 1, from y = 0 to y = 1, with a top cap.
// The side's uv wraps once around (u) and goes from v = sideTexHeight at the bottom to
// v = 0 at the top (so the side can use a strip of the texture other than its center);
// the cap maps the unit disc onto the texture.
class CylinderGeometry(
  val gl : WebGL2RenderingContext,
  val segments : Int = 32,
  val sideTexHeight : Float = 1f) : Geometry() {

  val positions = ArrayList<Float>()
  val normals = ArrayList<Float>()
  val texCoords = ArrayList<Float>()
  val indices = ArrayList<Short>()
  init {
    // side: a bottom and a top vertex per column, the seam column is duplicated for the uv
    for (i in 0..segments) {
      val phi = i.toFloat() / segments * 2f * PI.toFloat()
      for (y in listOf(0f, 1f)) {
        positions.addAll(listOf(cos(phi), y, sin(phi)))
        normals.addAll(listOf(cos(phi), 0f, sin(phi)))
        texCoords.addAll(listOf(i.toFloat() / segments, (1f - y) * sideTexHeight))
      }
    }
    for (i in 0 until segments) {
      val b = 2 * i
      indices.addAll(listOf(b, b + 1, b + 2, b + 1, b + 3, b + 2).map { it.toShort() })
    }
    // top cap: a center vertex and a fan around it
    val center = positions.size / 3
    positions.addAll(listOf(0f, 1f, 0f))
    normals.addAll(listOf(0f, 1f, 0f))
    texCoords.addAll(listOf(0.5f, 0.5f))
    for (i in 0..segments) {
      val phi = i.toFloat() / segments * 2f * PI.toFloat()
      positions.addAll(listOf(cos(phi), 1f, sin(phi)))
      normals.addAll(listOf(0f, 1f, 0f))
      texCoords.addAll(listOf(0.5f + 0.5f * cos(phi), 0.5f + 0.5f * sin(phi)))
    }
    for (i in 0 until segments) {
      indices.addAll(listOf(center, center + 1 + i, center + 2 + i).map { it.toShort() })
    }
  }

  val vertexBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( positions.toTypedArray() ),
      GL.STATIC_DRAW)
  }

  val vertexNormalBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( normals.toTypedArray() ),
      GL.STATIC_DRAW)
  }

  val vertexTexCoordBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( texCoords.toTypedArray() ),
      GL.STATIC_DRAW)
  }

  val indexBuffer = gl.createBuffer()
  val indexCount = indices.size
  init{
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER,
      Uint16Array( indices.toTypedArray() ),
      GL.STATIC_DRAW)
  }

  val inputLayout = gl.createVertexArray()
  init{
    gl.bindVertexArray(inputLayout)

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.enableVertexAttribArray(0)
    gl.vertexAttribPointer(0,
      3, GL.FLOAT, //< three pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.enableVertexAttribArray(1)
    gl.vertexAttribPointer(1,
      3, GL.FLOAT, //< three pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.enableVertexAttribArray(2)
    gl.vertexAttribPointer(2,
      2, GL.FLOAT, //< two pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindVertexArray(null)
  }

  override fun draw() {
    gl.bindVertexArray(inputLayout)
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)

    gl.drawElements(GL.TRIANGLES, indexCount, GL.UNSIGNED_SHORT, 0)
  }
}
