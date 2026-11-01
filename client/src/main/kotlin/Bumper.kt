import vision.gears.webglmath.Vec3

class Bumper(val radius : Float, val height : Float, groundY : Float, vararg meshes : Mesh) : GameObject(*meshes) {

  val restitution = 0.8f

  init {
    scale.set(radius, height, radius)
    position.y = groundY

    motion { _, _, interactors ->
      interactors.forEach {
        if (it is Ball) collideWithBall(it)
        if (it is Avatar) collideWithAvatar(it)
      }
      true
    }
  }

  fun collideWithBall(ball : Ball) {
    val normal = ball.position - position
    normal.y = 0f
    val dist = normal.length()
    val minDist = radius + ball.radius
    if (dist >= minDist || dist < 0.0001f) return
    normal.normalize()
    ball.position += normal * (minDist - dist)
    bounced(ball.velocity, normal, restitution)?.let { ball.velocity.set(it.x, it.y, it.z) }
  }

  fun collideWithAvatar(avatar : Avatar) {
    if (avatar.bottomY > position.y + height) return
    val normal = avatar.closestPointTo(position) - position
    normal.y = 0f
    val dist = normal.length()
    if (dist >= radius) return
    if (dist < 0.0001f) {
      normal.set(avatar.position.x - position.x, 0f, avatar.position.z - position.z)
    }
    normal.normalize()
    avatar.position += normal * (radius - dist)
    bounced(avatar.velocity, normal, restitution)?.let { avatar.setVelocity(it) }
  }
}
