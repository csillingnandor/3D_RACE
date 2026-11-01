import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.khronos.webgl.WebGLRenderingContext as GL
import vision.gears.webglmath.*
import kotlin.js.Date
import kotlin.math.*

class Scene (
  val gl : WebGL2RenderingContext,
  val overlay : HTMLDivElement,
  val canvas : HTMLCanvasElement) : UniformProvider("scene") {

  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "quad-vs.glsl")
  val fsBackground = Shader(gl, GL.FRAGMENT_SHADER, "background-fs.glsl")
  val backgroundMaterial = Material(Program(gl, vsQuad, fsBackground)).apply {
    this["envTexture"]?.set(TextureCube(gl,
      "media/posx512.jpg", "media/negx512.jpg",
      "media/posy512.jpg", "media/negy512.jpg",
      "media/posz512.jpg", "media/negz512.jpg"))
  }
  val backgroundMesh = Mesh(backgroundMaterial, TexturedQuadGeometry(gl))

  val jsonLoader = JsonLoader()
  val chevyChassisGeometries = jsonLoader.loadGeometries(gl, "media/json/json/chevy/chassis.json")
  val chevyWheelGeometries = jsonLoader.loadGeometries(gl, "media/json/json/chevy/wheel.json")
  val sphereGeometries = jsonLoader.loadGeometries(gl, "media/sphere.json")

  val vsTransform = Shader(gl, GL.VERTEX_SHADER, "transform-vs.glsl")
  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "textured-fs.glsl")
  val texturedProgram = Program(gl, vsTransform, fsTextured)

  fun texturedMaterial(texture : String, specular : Float, shininess : Float) = Material(texturedProgram).apply {
    this["colorTexture"]?.set(Texture2D(gl, texture))
    this["ks"]?.set(Vec3(specular, specular, specular))
    this["shininess"]?.set(shininess)
  }

  val chevyMaterial = texturedMaterial("media/json/json/chevy/chevy.png", 0.5f, 40f)
  val trackMaterial = texturedMaterial("media/tile.png", 0.1f, 10f)
  val finishMaterial = texturedMaterial("media/finish.png", 0.1f, 10f)
  val ballMaterial = texturedMaterial("media/ball.png", 0.8f, 60f)
  val bumperMaterial = texturedMaterial("media/bumper.png", 0.7f, 50f)
  val railMaterial = texturedMaterial("media/rail.png", 0.9f, 80f)

  val bumperGeometry = CylinderGeometry(gl, sideTexHeight = 0.3f)
  val railGeometry = CylinderGeometry(gl)

  fun meshes(geometries : List<Geometry>, material : Material) =
    geometries.map { Mesh(material, it) }.toTypedArray()

  val groundY = -11.5f
  val track = Track(arrayOf(
    "..............",
    ".S............",
    ".#............",
    ".#............",
    ".O............",
    ".#............",
    ".#............",
    ".#####O####...",
    "..........#...",
    "..........#...",
    "..........#...",
    "..........O...",
    "..........#...",
    "..........#...",
    "...########...",
    "...#..........",
    "...#..........",
    "...F..........",
    ".............."
  ), 60f, groundY)
  val startYaw = 0f

  val bumperPlaces = listOf(Triple(2, 7, 'N'), Triple(4, 7, 'S'), Triple(9, 7, 'N'), Triple(10, 9, 'W'),
                            Triple(10, 13, 'E'), Triple(7, 14, 'N'), Triple(5, 14, 'S'))
  val ballPlaces = listOf(Triple(1, 6, 0f), Triple(3, 7, 8f), Triple(8, 7, -8f), Triple(10, 9, 0f), Triple(6, 14, 10f))
  val wheelPlaces = listOf(Pair(7f, 14f), Pair(-7f, 14f), Pair(7f, -11f), Pair(-7f, -11f))

  val gameObjects = ArrayList<GameObject>()
  val chassis = Avatar(track, *meshes(chevyChassisGeometries, chevyMaterial))

  init {
    addRoad()
    addGuardrails()
    addBumpers()
    addCar()
  }

  fun addRoad() {
    gameObjects += GameObject(Mesh(trackMaterial,
        TrackGeometry(gl, track.cols, track.rows, track.tileSize) { i, j -> track.cell(i, j) !in "#S" })).apply {
      position.set(0f, groundY, 0f)
    }
    gameObjects += GameObject(Mesh(finishMaterial,
        TrackGeometry(gl, track.cols, track.rows, track.tileSize) { i, j -> track.cell(i, j) != 'F' })).apply {
      position.set(0f, groundY + 0.05f, 0f)
    }
  }

  fun addGuardrails() {
    val railRadius = 1.2f
    for (wall in track.walls) {
      for (height in listOf(3.5f, 7f)) {
        gameObjects += GameObject(Mesh(railMaterial, railGeometry)).apply {
          scale.set(railRadius, wall.length, railRadius)
          roll = -PI.toFloat() / 2f
          yaw = if (wall.alongX) 0f else -PI.toFloat() / 2f
          position.set(wall.x0 - wall.normal.x * railRadius, groundY + height, wall.z0 - wall.normal.z * railRadius)
        }
      }
    }
    for (p in track.railPosts) {
      gameObjects += GameObject(Mesh(railMaterial, railGeometry)).apply {
        scale.set(1.5f, 8f, 1.5f)
        position.set(p.x, groundY, p.z)
      }
    }
  }

  fun addBumpers() {
    for ((i, j, side) in bumperPlaces) {
      val p = track.wallMidpoint(i, j, side)
      gameObjects += Bumper(7f, 8f, groundY, Mesh(bumperMaterial, bumperGeometry)).apply {
        position.x = p.x
        position.z = p.z
      }
    }
  }

  fun addCar() {
    gameObjects += chassis
    for ((x, z) in wheelPlaces) {
      gameObjects += Wheel(chassis, mirrored = x < 0f, steerable = z > 0f,
          *meshes(chevyWheelGeometries, chevyMaterial)).apply {
        position.set(x, -3.5f, z)
      }
    }
  }

  fun resetBalls() {
    gameObjects.removeAll { it is Ball }
    for ((i, j, offset) in ballPlaces) {
      val c = track.cellCenter(i, j)
      gameObjects += Ball(5f, track, *meshes(sphereGeometries, ballMaterial)).apply {
        position.x = c.x + offset
        position.z = c.z + offset
      }
    }
  }

  val lights = Array<Light>(1) { Light(it) }
  init{
    lights[0].position.set(1.0f, 1.0f, 1.0f, 0.0f).normalize()
    lights[0].powerDensity.set(2.0f, 2.0f, 2.0f)
  }

  val camera = PerspectiveCamera()
  val heliCam = HeliCam(camera, chassis)

  var raceTime = 0f
  var finished = false
  var message = ""
  var messageTime = 0f

  fun resetRace() {
    chassis.reset(track.startCenter, startYaw)
    heliCam.reset()
    resetBalls()
    raceTime = 0f
    finished = false
  }
  init {
    resetRace()
  }

  var overlayHtml = ""
  fun updateOverlay() {
    val time = raceTime.asDynamic().toFixed(1) as String
    val html = if (finished) {
      """<div style="font-size:36px">CÉL! Időd: $time s</div><div>R: új futam</div>"""
    } else {
      """<div style="font-size:24px">Idő: $time s</div>""" +
      (if (messageTime > 0f) """<div style="font-size:24px;color:#f66">$message</div>""" else "") +
      """<div>↑↓ gáz / fék &nbsp; ←→ kormány &nbsp; SPACE ugrás &nbsp; R újraindítás</div>"""
    }
    val styled = """<div style="color:white;font:bold 14px sans-serif;text-shadow:0 0 4px #000">$html</div>"""
    if (styled != overlayHtml) {
      overlayHtml = styled
      overlay.innerHTML = styled
    }
  }

  fun updateRace(dt : Float, keysPressed : Set<String>) {
    if ("R" in keysPressed) {
      resetRace()
    }
    if (chassis.position.y < groundY - 150f) {
      resetRace()
      message = "Leestél! Újra a rajttól."
      messageTime = 2f
    }
    if (!finished) {
      raceTime += dt
      if (chassis.onGround && track.isFinish(chassis.position.x, chassis.position.z)) {
        finished = true
      }
    }
    messageTime -= dt
    updateOverlay()
  }

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  init{
    gl.enable(GL.DEPTH_TEST)
  }

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
    camera.setAspectRatio(canvas.width.toFloat() / canvas.height.toFloat())
  }

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>, timeStamp : Double) {
    val timeAtThisFrame = Date().getTime()
    val dt = min((timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f, 0.05f)
    val t  = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)

    val spawn = ArrayList<GameObject>()
    val killList = ArrayList<GameObject>()
    gameObjects.forEach {
      if(!it.move(dt, t, keysPressed, gameObjects, spawn)){
        killList.add(it)
      }
    }
    killList.forEach{ gameObjects.remove(it) }
    spawn.forEach{ gameObjects.add(it) }
    gameObjects.forEach {
      if (it is Avatar) track.collideWithAvatar(it)
      if (it is Ball) track.collideWithBall(it)
    }

    updateRace(dt, keysPressed)

    gameObjects.forEach { it.update() }
    heliCam.move(dt)

    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
    backgroundMesh.draw(camera)
    gameObjects.forEach { it.draw( camera, *lights ) }
  }
}
