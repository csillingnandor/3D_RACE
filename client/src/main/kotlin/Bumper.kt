import vision.gears.webglmath.Vec3
import kotlin.math.*

// Static upright cylinder that kicks back whatever hits it with extra energy:
// the normal velocity is reflected with a restitution above 1, so the kick back
// is proportional to the impact speed.
// Collisions are tested in the ground plane (a circle against the ball's circle
// or the car's box).
class Bumper(val radius : Float, height : Float, groundY : Float, vararg meshes : Mesh) : GameObject(*meshes) {

  val restitution = 0.8f // outgoing / incoming normal speed

  init {
    scale.set(radius, height, radius)
    position.y = groundY

    move = object : GameObject.Motion(this) {
      override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
          interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
        interactors.forEach {
          if (it is Ball) collideWithBall(it)
          if (it is Avatar) collideWithAvatar(it)
        }
        return true
      }
    }
  }

  // reflected normal velocity with extra energy; normal points from the bumper outward
  private fun bounce(velocity : Vec3, normal : Vec3) : Vec3 {
    val normalSpeed = velocity.x * normal.x + velocity.z * normal.z
    if (normalSpeed >= 0f) return velocity // already moving away
    return velocity - normal * ((1f + restitution) * normalSpeed)
  }

  fun collideWithBall(ball : Ball) {
    val normal = ball.position - position
    normal.y = 0f
    val dist = normal.length()
    val minDist = radius + ball.radius
    if (dist >= minDist || dist < 0.0001f) return
    normal.normalize()
    ball.position += normal * (minDist - dist)
    val v = bounce(ball.velocity, normal)
    ball.velocity.set(v.x, v.y, v.z)
  }

  fun collideWithAvatar(avatar : Avatar) {
    // closest point of the car's ground-plane box to the bumper's axis
    val d = position - avatar.position
    d.y = 0f
    val localX = (d.x * avatar.side.x + d.z * avatar.side.z).coerceIn(-avatar.halfWidth, avatar.halfWidth)
    val localZ = (d.x * avatar.ahead.x + d.z * avatar.ahead.z).coerceIn(-avatar.halfLength, avatar.halfLength)
    val closest = avatar.side * localX + avatar.ahead * localZ // relative to the car
    val normal = closest - d
    normal.y = 0f
    val dist = normal.length()
    if (dist >= radius) return
    if (dist < 0.0001f) {
      normal.set(-d.x, 0f, -d.z) // axis inside the box: push the car straight away
    }
    normal.normalize()
    avatar.position += normal * (radius - dist)
    avatar.setVelocity(bounce(avatar.velocity, normal))
  }
}
