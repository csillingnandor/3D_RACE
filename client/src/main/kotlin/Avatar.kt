import vision.gears.webglmath.Vec3
import kotlin.math.*

// Drivable car: UP/DOWN accelerate/brake, LEFT/RIGHT steer.
// Collides as an oriented box in the ground plane (see Ball).
class Avatar(vararg meshes : Mesh) : GameObject(*meshes) {

  var speed = 0f
  var steerAngle = 0f         // of the front wheels, positive is left
  val maxSteerAngle = 0.6f
  val wheelBase = 25f         // distance between front and rear axles
  val halfWidth = 9f
  val halfLength = 21f

  // the chevy's front is its model-space +z axis
  val ahead get() = Vec3(sin(yaw), 0f, cos(yaw))
  val side get() = Vec3(cos(yaw), 0f, -sin(yaw))
  // sideways sliding after a collision; the tyres' grip makes it die off
  val knockback = Vec3()
  val velocity get() = ahead * speed + knockback

  // changes the car's velocity: the part along the heading goes into speed, the rest slides
  fun setVelocity(v : Vec3) {
    speed = v.x * ahead.x + v.z * ahead.z
    knockback.set(v.x - ahead.x * speed, 0f, v.z - ahead.z * speed)
  }

  init {
    move = object : GameObject.Motion(this) {
      override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
          interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
        // the engine only works up to the limits, a bounce may push speed beyond them
        if("UP" in keysPressed && speed < 150f) { speed += 100f * dt }
        if("DOWN" in keysPressed && speed > -30f) { speed -= 40f * dt }
        speed -= speed * 0.8f * dt // rolling resistance
        knockback *= max(0f, 1f - 2f * dt)

        var targetSteer = 0f
        if("LEFT" in keysPressed) { targetSteer += maxSteerAngle }
        if("RIGHT" in keysPressed) { targetSteer -= maxSteerAngle }
        // the front wheels turn towards the target angle instead of snapping to it
        steerAngle += (targetSteer - steerAngle) * min(1f, 8f * dt)
        yaw += speed * tan(steerAngle) / wheelBase * dt

        position += velocity * dt
        return true
      }
    }
  }
}
