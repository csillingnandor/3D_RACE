import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import kotlin.math.*

class Ball(val radius : Float, val track : Track, vararg meshes : Mesh) : GameObject(*meshes) {

  val velocity = Vec3()
  val orientation = Mat4()
  val restitution = 0.8f
  val restY get() = track.groundY + radius
  var verticalSpeed = 0f

  init {
    scale.set(radius, radius, radius)
    position.y = restY

    motion { dt, _, interactors ->
      interactors.forEach {
        if (it is Avatar) collideWithAvatar(it)
        if (it is Ball && interactors.indexOf(this) < interactors.indexOf(it)) collideWithBall(it)
      }

      velocity *= max(0f, 1f - 0.4f * dt)
      position += velocity * dt

      verticalSpeed -= GRAVITY * dt
      position.y += verticalSpeed * dt
      if (track.supports(position, restY)) {
        position.y = restY
        verticalSpeed = 0f
      }

      val speed = velocity.length()
      if (speed > 0.0001f) {
        orientation *= Mat4().rotate(speed * dt / radius, velocity.z, 0f, -velocity.x)
      }
      position.y > track.groundY - 200f
    }
  }

  override fun applyRotation(m : Mat4) {
    m *= orientation
  }

  fun collideWithAvatar(avatar : Avatar) {
    if (abs((position.y - radius) - avatar.bottomY) > 8f) return
    val normal = position - avatar.closestPointTo(position)
    normal.y = 0f
    val dist = normal.length()
    if (dist >= radius) return
    if (dist < 0.0001f) {
      normal.set(position.x - avatar.position.x, 0f, position.z - avatar.position.z)
    }
    normal.normalize()
    position += normal * (radius - dist)

    val carVelocity = avatar.velocity
    bounced(velocity - carVelocity, normal, restitution)?.let {
      val v = it + carVelocity
      velocity.set(v.x, v.y, v.z)
    }
  }

  fun collideWithBall(other : Ball) {
    val normal = other.position - position
    normal.y = 0f
    val dist = normal.length()
    val minDist = radius + other.radius
    if (dist >= minDist || dist < 0.0001f) return
    normal.normalize()
    position -= normal * ((minDist - dist) * 0.5f)
    other.position += normal * ((minDist - dist) * 0.5f)

    val approachSpeed = normalSpeed(velocity - other.velocity, normal)
    if (approachSpeed > 0f) {
      val impulse = normal * ((1f + restitution) * 0.5f * approachSpeed)
      velocity -= impulse
      other.velocity += impulse
    }
  }
}
