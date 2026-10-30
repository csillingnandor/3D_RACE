import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import vision.gears.webglmath.Geometry

// Flat track in the y = 0 plane, made of tilesX * tilesZ square tiles centered on the origin.
// Every tile has its own four vertices with uv 0..1, so the texture repeats per tile and
// tiles can be left out (isHole) without touching their neighbours.
class TrackGeometry(
  val gl : WebGL2RenderingContext,
  val tilesX : Int,
  val tilesZ : Int,
  val tileSize : Float,
  val isHole : (Int, Int) -> Boolean = { _, _ -> false }
  ) : Geometry() {

  val positions = ArrayList<Float>()
  val normals = ArrayList<Float>()
  val texCoords = ArrayList<Float>()
  val indices = ArrayList<Short>()
  init {
    for (i in 0 until tilesX) {
      for (j in 0 until tilesZ) {
        if (isHole(i, j)) continue
        val x0 = (i - tilesX * 0.5f) * tileSize
        val z0 = (j - tilesZ * 0.5f) * tileSize
        val base = positions.size / 3
        for ((u, v) in listOf(Pair(0f, 0f), Pair(1f, 0f), Pair(0f, 1f), Pair(1f, 1f))) {
          positions.addAll(listOf(x0 + u * tileSize, 0f, z0 + v * tileSize))
          normals.addAll(listOf(0f, 1f, 0f))
          texCoords.addAll(listOf(u, v))
        }
        // counter-clockwise when seen from above
        indices.addAll(listOf(base, base + 2, base + 1, base + 1, base + 2, base + 3).map { it.toShort() })
      }
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
