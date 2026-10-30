import vision.gears.webglmath.Vec3
import kotlin.math.*

// Drivable car: UP/DOWN accelerate/brake, LEFT/RIGHT steer.
// Collides as an oriented box in the ground plane (see Ball).
class Avatar(vararg meshes : Mesh) : GameObject(*meshes) {

  var speed = 0f
  val halfWidth = 9f
  val halfLength = 21f

  // the chevy's front is its model-space +z axis
  val ahead get() = Vec3(sin(yaw), 0f, cos(yaw))
  val side get() = Vec3(cos(yaw), 0f, -sin(yaw))
  val velocity get() = ahead * speed

  init {
    move = object : GameObject.Motion(this) {
      override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
          interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
        if("UP" in keysPressed) { speed += 100f * dt }
        if("DOWN" in keysPressed) { speed -= 40f * dt }
        speed -= speed * 0.8f * dt // rolling resistance
        speed = speed.coerceIn(-30f, 150f)

        var steer = 0f
        if("LEFT" in keysPressed) { steer += 0.6f }
        if("RIGHT" in keysPressed) { steer -= 0.6f }
        val wheelBase = 25f
        yaw += speed * tan(steer) / wheelBase * dt

        position += ahead * (speed * dt)
        return true
      }
    }
  }
}
