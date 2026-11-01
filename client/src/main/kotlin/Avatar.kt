import vision.gears.webglmath.Vec2
import vision.gears.webglmath.Vec3
import kotlin.math.*

class Avatar(val track : Track, vararg meshes : Mesh) : GameObject(*meshes) {

  var speed = 0f
  var steerAngle = 0f
  val maxSteerAngle = 0.6f
  val wheelBase = 25f
  val halfWidth = 9f
  val halfLength = 21f

  val groundClearance = 6.5f
  val restY get() = track.groundY + groundClearance
  val bottomY get() = position.y - groundClearance
  var verticalSpeed = 0f
  var onGround = true
  val jumpSpeed = 55f

  val ahead get() = Vec3(sin(yaw), 0f, cos(yaw))
  val side get() = Vec3(cos(yaw), 0f, -sin(yaw))
  val knockback = Vec3()
  val velocity get() = ahead * speed + knockback

  fun setVelocity(v : Vec3) {
    speed = v.x * ahead.x + v.z * ahead.z
    knockback.set(v.x - ahead.x * speed, 0f, v.z - ahead.z * speed)
  }

  fun toLocal(p : Vec3) : Vec2 {
    val dx = p.x - position.x
    val dz = p.z - position.z
    return Vec2(dx * side.x + dz * side.z, dx * ahead.x + dz * ahead.z)
  }

  fun closestPointTo(p : Vec3) : Vec3 {
    val local = toLocal(p)
    return position +
      side * local.x.coerceIn(-halfWidth, halfWidth) +
      ahead * local.y.coerceIn(-halfLength, halfLength)
  }

  fun corners() : List<Vec3> = listOf(-1f, 1f).flatMap { sx -> listOf(-1f, 1f).map { sz ->
    position + side * (sx * halfWidth) + ahead * (sz * halfLength)
  } }

  fun reset(start : Vec3, startYaw : Float) {
    position.set(start.x, restY, start.z)
    yaw = startYaw
    speed = 0f
    steerAngle = 0f
    knockback.set()
    verticalSpeed = 0f
    onGround = true
  }

  init {
    position.y = restY
    motion { dt, keysPressed, _ ->
      if (onGround) {
        if ("UP" in keysPressed && speed < 150f) { speed += 100f * dt }
        if ("DOWN" in keysPressed && speed > -30f) { speed -= 40f * dt }
        if ("SPACE" in keysPressed) {
          verticalSpeed = jumpSpeed
          onGround = false
        }
        speed -= speed * 0.8f * dt
        knockback *= max(0f, 1f - 2f * dt)
      }

      var targetSteer = 0f
      if ("LEFT" in keysPressed) { targetSteer += maxSteerAngle }
      if ("RIGHT" in keysPressed) { targetSteer -= maxSteerAngle }
      steerAngle += (targetSteer - steerAngle) * min(1f, 8f * dt)
      if (onGround) {
        yaw += speed * tan(steerAngle) / wheelBase * dt
      }

      position += velocity * dt

      verticalSpeed -= GRAVITY * dt
      position.y += verticalSpeed * dt
      onGround = track.supports(position, restY)
      if (onGround) {
        position.y = restY
        verticalSpeed = 0f
      }
      true
    }
  }
}
