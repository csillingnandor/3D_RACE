import vision.gears.webglmath.Vec3

const val GRAVITY = 100f

fun normalSpeed(velocity : Vec3, normal : Vec3) = velocity.x * normal.x + velocity.z * normal.z

fun bounced(velocity : Vec3, normal : Vec3, restitution : Float) : Vec3? {
  val speed = normalSpeed(velocity, normal)
  return if (speed < 0f) velocity - normal * ((1f + restitution) * speed) else null
}
