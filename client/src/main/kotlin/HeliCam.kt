import vision.gears.webglmath.Vec3
import kotlin.math.*

// Invisible, physically simulated helicopter that carries the camera.
// A damped spring force pulls it above-behind the target, a damped spring torque
// turns it to face the target.
class HeliCam(val camera : PerspectiveCamera, val target : GameObject) {

  val position = Vec3(0f, 20f, 40f)
  val velocity = Vec3()
  val mass = 1f
  val springStiffness = 40f   // [force / distance]
  val linearDrag = 10f        // momentum damping

  var yaw = 0f
  var pitch = 0f
  var yawRate = 0f
  var pitchRate = 0f
  val inertia = 1f
  val torqueStiffness = 80f   // [torque / angle]
  val angularDrag = 14f       // angular momentum damping

  var distanceBehind = 45f
  var heightAbove = 18f

  fun move(dt : Float) {
    // the chevy's front is its model-space +z axis
    val targetAhead = Vec3(sin(target.yaw), 0f, cos(target.yaw))
    val restPosition = target.position - targetAhead * distanceBehind + Vec3(0f, heightAbove, 0f)

    val force = (restPosition - position) * springStiffness - velocity * linearDrag
    velocity += force * (dt / mass)
    position += velocity * dt

    val lookDir = (target.position + Vec3(0f, 3f, 0f) - position).normalize()
    val desiredYaw = atan2(-lookDir.x, -lookDir.z)
    val desiredPitch = asin(lookDir.y)

    val yawTorque = wrapAngle(desiredYaw - yaw) * torqueStiffness - yawRate * angularDrag
    val pitchTorque = (desiredPitch - pitch) * torqueStiffness - pitchRate * angularDrag
    yawRate += yawTorque * (dt / inertia)
    pitchRate += pitchTorque * (dt / inertia)
    yaw = wrapAngle(yaw + yawRate * dt)
    pitch += pitchRate * dt

    camera.position.set(position.x, position.y, position.z)
    camera.yaw = yaw
    camera.pitch = pitch
    camera.roll = 0f
    camera.update()
  }

  private fun wrapAngle(a : Float) : Float {
    var r = a
    while (r > PI.toFloat()) r -= 2f * PI.toFloat()
    while (r < -PI.toFloat()) r += 2f * PI.toFloat()
    return r
  }
}
