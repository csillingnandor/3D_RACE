import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import vision.gears.webglmath.Geometry

class VertexAttribute(val data : Array<Float>, val size : Int)

class GeometryData {
  val positions = ArrayList<Float>()
  val normals = ArrayList<Float>()
  val texCoords = ArrayList<Float>()
  val indices = ArrayList<Short>()

  val vertexCount get() = positions.size / 3

  fun vertex(x : Float, y : Float, z : Float, nx : Float, ny : Float, nz : Float, u : Float, v : Float) : Int {
    positions.addAll(listOf(x, y, z))
    normals.addAll(listOf(nx, ny, nz))
    texCoords.addAll(listOf(u, v))
    return vertexCount - 1
  }

  fun triangle(a : Int, b : Int, c : Int) {
    indices.addAll(listOf(a.toShort(), b.toShort(), c.toShort()))
  }

  fun attributes() = listOf(
    VertexAttribute(positions.toTypedArray(), 3),
    VertexAttribute(normals.toTypedArray(), 3),
    VertexAttribute(texCoords.toTypedArray(), 2))
}

open class IndexedGeometry(
  val gl : WebGL2RenderingContext,
  attributes : List<VertexAttribute>,
  indices : Array<Short>) : Geometry() {

  constructor(gl : WebGL2RenderingContext, data : GeometryData) :
    this(gl, data.attributes(), data.indices.toTypedArray())

  val indexCount = indices.size
  val indexBuffer = gl.createBuffer()
  val inputLayout = gl.createVertexArray()

  init {
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, Uint16Array(indices), GL.STATIC_DRAW)

    gl.bindVertexArray(inputLayout)
    attributes.forEachIndexed { location, attribute ->
      gl.bindBuffer(GL.ARRAY_BUFFER, gl.createBuffer())
      gl.bufferData(GL.ARRAY_BUFFER, Float32Array(attribute.data), GL.STATIC_DRAW)
      gl.enableVertexAttribArray(location)
      gl.vertexAttribPointer(location, attribute.size, GL.FLOAT, false, 0, 0)
    }
    gl.bindVertexArray(null)
  }

  override fun draw() {
    gl.bindVertexArray(inputLayout)
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.drawElements(GL.TRIANGLES, indexCount, GL.UNSIGNED_SHORT, 0)
  }
}
