import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import kotlin.math.*

// Ball rolling on the ground (y = groundY) without slipping: the angular velocity
// is always up x velocity / radius, accumulated into an orientation matrix.
// The avatar can push it; balls also bounce off each other.
class Ball(val radius : Float, groundY : Float, vararg meshes : Mesh) : GameObject(*meshes) {

  val velocity = Vec3()
  val orientation = Mat4()
  val restitution = 0.8f

  init {
    scale.set(radius, radius, radius)
    position.y = groundY + radius

    move = object : GameObject.Motion(this) {
      override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
          interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
        interactors.forEach {
          if (it is Avatar) collideWithAvatar(it)
          // each pair is resolved once, by the ball that comes first in the list
          if (it is Ball && it !== this@Ball &&
              interactors.indexOf(this@Ball) < interactors.indexOf(it)) collideWithBall(it)
        }

        velocity *= max(0f, 1f - 0.4f * dt) // rolling resistance
        position += velocity * dt

        val speed = velocity.length()
        if (speed > 0.0001f) {
          // axis = up x velocity
          orientation *= Mat4().rotate(speed * dt / radius, velocity.z, 0f, -velocity.x)
        }
        return true
      }
    }
  }

  override fun update() {
    modelMatrix.set().
      scale(scale)
    modelMatrix *= orientation
    modelMatrix.translate(position)
    modelMatrixInverse.set(modelMatrix).invert()
  }

  fun collideWithAvatar(avatar : Avatar) {
    // closest point of the car's ground-plane box to the ball center
    val d = position - avatar.position
    d.y = 0f
    val localX = (d.x * avatar.side.x + d.z * avatar.side.z).coerceIn(-avatar.halfWidth, avatar.halfWidth)
    val localZ = (d.x * avatar.ahead.x + d.z * avatar.ahead.z).coerceIn(-avatar.halfLength, avatar.halfLength)
    val closest = avatar.position + avatar.side * localX + avatar.ahead * localZ
    val normal = position - closest
    normal.y = 0f
    val dist = normal.length()
    if (dist >= radius) return
    if (dist < 0.0001f) {
      normal.set(d.x, 0f, d.z)
    }
    normal.normalize()
    position += normal * (radius - dist)

    // the car is treated as infinitely heavy
    val relativeNormalSpeed = (velocity.x - avatar.velocity.x) * normal.x +
                              (velocity.z - avatar.velocity.z) * normal.z
    if (relativeNormalSpeed < 0f) {
      velocity -= normal * ((1f + restitution) * relativeNormalSpeed)
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

    // equal masses
    val relativeNormalSpeed = (velocity.x - other.velocity.x) * normal.x +
                              (velocity.z - other.velocity.z) * normal.z
    if (relativeNormalSpeed > 0f) {
      val impulse = normal * ((1f + restitution) * 0.5f * relativeNormalSpeed)
      velocity -= impulse
      other.velocity += impulse
    }
  }
}
