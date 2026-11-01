import kotlin.math.*

class Wheel(
  val avatar : Avatar,
  val mirrored : Boolean,
  val steerable : Boolean,
  vararg meshes : Mesh) : GameObject(*meshes) {

  val radius = 3f

  init {
    parent = avatar
    motion { dt, _, _ ->
      val spin = avatar.speed * dt / radius
      pitch = (pitch + if (mirrored) -spin else spin) % (2f * PI.toFloat())
      yaw = (if (mirrored) PI.toFloat() else 0f) + (if (steerable) avatar.steerAngle else 0f)
      true
    }
  }
}
