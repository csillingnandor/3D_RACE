import kotlin.math.*

// Wheel attached to an avatar: spins around its axle (local x) to match the car's speed,
// and steerable wheels turn around the vertical axis with the avatar's steering angle.
// Mirrored wheels (left side) are turned around by 180 degrees so the rim faces outward,
// which also flips their spin direction.
class Wheel(
  val avatar : Avatar,
  val mirrored : Boolean,
  val steerable : Boolean,
  vararg meshes : Mesh) : GameObject(*meshes) {

  val radius = 3f

  init {
    parent = avatar
    move = object : GameObject.Motion(this) {
      override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
          interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
        // rolling without slipping: angle = distance / radius
        val spin = avatar.speed * dt / radius
        pitch = (pitch + if (mirrored) -spin else spin) % (2f * PI.toFloat())
        yaw = (if (mirrored) PI.toFloat() else 0f) + (if (steerable) avatar.steerAngle else 0f)
        return true
      }
    }
  }
}
