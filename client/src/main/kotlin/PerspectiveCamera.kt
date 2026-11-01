import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import kotlin.math.*

class PerspectiveCamera() : UniformProvider("camera") {

  val position by Vec3(0.0f, 0.0f, 1.0f)
  var roll = 0.0f
  var pitch = 0.0f
  var yaw = 0.0f

  var fov = 1.0f
  var aspect = 1.0f
  var nearPlane = 1.0f
  var farPlane = 10000.0f

  val rotationMatrix = Mat4()
  val viewProjMatrix by Mat4()
  val rayDirMatrix by Mat4()

  init {
    update()
  }

  fun update() {
    rotationMatrix.set().
      rotate(roll).
      rotate(pitch, 1.0f, 0.0f, 0.0f).
      rotate(yaw, 0.0f, 1.0f, 0.0f)
    viewProjMatrix.set(rotationMatrix).
      translate(position).
      invert()

    val yScale = 1.0f / tan(fov * 0.5f)
    val xScale = yScale / aspect
    val f = farPlane
    val n = nearPlane
    viewProjMatrix *= Mat4(
        xScale ,    0.0f ,         0.0f ,   0.0f,
          0.0f ,  yScale ,         0.0f ,   0.0f,
          0.0f ,    0.0f ,  (n+f)/(n-f) ,  -1.0f,
          0.0f ,    0.0f ,  2*n*f/(n-f) ,   0.0f)

    rayDirMatrix.set().translate(position)
    rayDirMatrix *= viewProjMatrix
    rayDirMatrix.invert()
  }

  fun setAspectRatio(ar : Float) {
    aspect = ar
    update()
  }
}
