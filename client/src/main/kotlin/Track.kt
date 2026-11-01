import vision.gears.webglmath.Vec3
import kotlin.math.*

class Track(val layout : Array<String>, val tileSize : Float, val groundY : Float) {

  val rows = layout.size
  val cols = layout[0].length

  fun cell(i : Int, j : Int) : Char =
    if (j in 0 until rows && i in 0 until cols) layout[j][i] else '.'
  fun isTrack(i : Int, j : Int) = cell(i, j) in "#SFO"
  fun hasSurface(i : Int, j : Int) = cell(i, j) in "#SF"

  fun cellI(x : Float) = floor(x / tileSize + cols * 0.5f).toInt()
  fun cellJ(z : Float) = floor(z / tileSize + rows * 0.5f).toInt()
  fun isGround(x : Float, z : Float) = hasSurface(cellI(x), cellJ(z))
  fun supports(p : Vec3, restY : Float) = isGround(p.x, p.z) && p.y <= restY && p.y > restY - 3f
  fun isFinish(x : Float, z : Float) = cell(cellI(x), cellJ(z)) == 'F'

  fun cellMinX(i : Int) = (i - cols * 0.5f) * tileSize
  fun cellMinZ(j : Int) = (j - rows * 0.5f) * tileSize
  fun cellCenter(i : Int, j : Int) =
    Vec3(cellMinX(i) + tileSize * 0.5f, groundY, cellMinZ(j) + tileSize * 0.5f)

  val startCenter : Vec3
  init {
    val j = layout.indexOfFirst { 'S' in it }
    startCenter = cellCenter(layout[j].indexOf('S'), j)
  }

  class Wall(val x0 : Float, val z0 : Float, val x1 : Float, val z1 : Float, val normal : Vec3) {
    val length get() = max(x1 - x0, z1 - z0)
    val alongX get() = z0 == z1
  }
  val walls = ArrayList<Wall>()
  init {
    for (j in 0 until rows) {
      for (i in 0 until cols) {
        if (!isTrack(i, j)) continue
        val x0 = cellMinX(i); val x1 = x0 + tileSize
        val z0 = cellMinZ(j); val z1 = z0 + tileSize
        if (!isTrack(i - 1, j)) walls += Wall(x0, z0, x0, z1, Vec3(1f, 0f, 0f))
        if (!isTrack(i + 1, j)) walls += Wall(x1, z0, x1, z1, Vec3(-1f, 0f, 0f))
        if (!isTrack(i, j - 1)) walls += Wall(x0, z0, x1, z0, Vec3(0f, 0f, 1f))
        if (!isTrack(i, j + 1)) walls += Wall(x0, z1, x1, z1, Vec3(0f, 0f, -1f))
      }
    }
  }

  val railPosts = ArrayList<Vec3>()
  val convexCorners = ArrayList<Vec3>()
  init {
    for (gj in 0..rows) {
      for (gi in 0..cols) {
        val around = listOf(isTrack(gi - 1, gj - 1), isTrack(gi, gj - 1), isTrack(gi - 1, gj), isTrack(gi, gj))
        val trackCount = around.count { it }
        if (trackCount == 0 || trackCount == 4) continue
        val p = Vec3(cellMinX(gi), groundY, cellMinZ(gj))
        railPosts += p
        if (trackCount == 3) convexCorners += p
      }
    }
  }

  fun wallMidpoint(i : Int, j : Int, side : Char) : Vec3 {
    val c = cellCenter(i, j)
    val h = tileSize * 0.5f
    when (side) {
      'N' -> c.z -= h
      'S' -> c.z += h
      'W' -> c.x -= h
      'E' -> c.x += h
    }
    return c
  }

  val wallRestitution = 0.3f

  fun collideWithAvatar(avatar : Avatar) {
    val corners = avatar.corners()
    val maxDepth = 15f
    for (wall in walls) {
      val wallOffset = wall.normal.x * wall.x0 + wall.normal.z * wall.z0
      var depth = 0f
      for (c in corners) {
        val inRange = if (wall.alongX) c.x in wall.x0..wall.x1 else c.z in wall.z0..wall.z1
        if (!inRange) continue
        val d = wallOffset - (wall.normal.x * c.x + wall.normal.z * c.z)
        if (d > depth && d < maxDepth) depth = d
      }
      if (depth > 0f) pushAvatar(avatar, wall.normal, depth)
    }
    for (p in convexCorners) {
      val local = avatar.toLocal(p)
      val penX = avatar.halfWidth - abs(local.x)
      val penZ = avatar.halfLength - abs(local.y)
      if (penX <= 0f || penZ <= 0f) continue
      if (penX < penZ) {
        pushAvatar(avatar, avatar.side * (if (local.x > 0f) -1f else 1f), penX)
      } else {
        pushAvatar(avatar, avatar.ahead * (if (local.y > 0f) -1f else 1f), penZ)
      }
    }
  }

  private fun pushAvatar(avatar : Avatar, normal : Vec3, depth : Float) {
    avatar.position += normal * depth
    bounced(avatar.velocity, normal, wallRestitution)?.let { avatar.setVelocity(it) }
  }

  fun collideWithBall(ball : Ball) {
    for (wall in walls) {
      val qx = ball.position.x.coerceIn(wall.x0, wall.x1)
      val qz = ball.position.z.coerceIn(wall.z0, wall.z1)
      val n = Vec3(ball.position.x - qx, 0f, ball.position.z - qz)
      val dist = n.length()
      if (dist >= ball.radius) continue
      if (dist < 0.0001f) n.set(wall.normal.x, 0f, wall.normal.z) else n.normalize()
      ball.position += n * (ball.radius - dist)
      bounced(ball.velocity, n, wallRestitution)?.let { ball.velocity.set(it.x, it.y, it.z) }
    }
  }
}
