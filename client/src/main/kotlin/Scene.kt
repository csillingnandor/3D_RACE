import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint32Array
import vision.gears.webglmath.*
import kotlin.js.Date
import kotlin.math.*
import kotlin.random.*

class Scene (
  val gl : WebGL2RenderingContext,
  val overlay : HTMLDivElement,
  val canvas : HTMLCanvasElement) : UniformProvider("scene") {

  lateinit var defaultFramebuffer : DefaultFramebuffer
  // LABTODO: postProcFramebuffer
  var frameCount = 0

  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "quad-vs.glsl")
  val fsBackground = Shader(gl, GL.FRAGMENT_SHADER, "background-fs.glsl")

  val fsShow = Shader(gl, GL.FRAGMENT_SHADER, "show-fs.glsl")

  val quadGeometry = TexturedQuadGeometry(gl)
  val flipQuadGeometry = FlipQuadGeometry(gl)
  //LABTODO: resources for multipass rendering

  val showMesh = Mesh(Material(Program(gl, vsQuad, fsShow)), flipQuadGeometry)

  val backgroundProgram = Program(gl, vsQuad, fsBackground)
  val backgroundMaterial = Material(backgroundProgram)
  val skyCubeTexture = TextureCube(gl,
      "media/posx512.jpg", "media/negx512.jpg",
      "media/posy512.jpg", "media/negy512.jpg",
      "media/posz512.jpg", "media/negz512.jpg"
    )
  init {
    backgroundMaterial["envTexture"]?.set( skyCubeTexture )
  }
  val backgroundMesh = Mesh(backgroundMaterial, quadGeometry)

  val jsonLoader = JsonLoader()
  val exhibitGeometries = jsonLoader.loadGeometries(gl, "media/slowpoke/Slowpoke.json")
  val chevyChassisGeometries = jsonLoader.loadGeometries(gl, "media/json/json/chevy/chassis.json")
  val chevyWheelGeometries = jsonLoader.loadGeometries(gl, "media/json/json/chevy/wheel.json")
  val heliBodyGeometries = jsonLoader.loadGeometries(gl, "media/json/json/heli/heli1.json")
  val heliMainRotorGeometries = jsonLoader.loadGeometries(gl, "media/json/json/heli/mainrotor.json")
  val heliTailRotorGeometries = jsonLoader.loadGeometries(gl, "media/json/json/heli/tailrotor.json")
  val sphereGeometries = jsonLoader.loadGeometries(gl, "media/sphere.json")

  val vsTransform = Shader(gl, GL.VERTEX_SHADER, "transform-vs.glsl")
  val fsPhong = Shader(gl, GL.FRAGMENT_SHADER, "phong-fs.glsl")
  val phongProgram = Program(gl, vsTransform, fsPhong)
  val phongMaterial = Material(phongProgram).apply {
    this["kd"]?.set(Vec3(0.6f, 0.2f, 0.1f))
    this["ks"]?.set(Vec3(1f, 1f, 1f))
    this["shininess"]?.set(15f)
  }

  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "textured-fs.glsl")
  val texturedProgram = Program(gl, vsTransform, fsTextured)
  val chevyMaterial = Material(texturedProgram).apply {
    this["colorTexture"]?.set(Texture2D(gl, "media/json/json/chevy/chevy.png"))
    this["ks"]?.set(Vec3(0.5f, 0.5f, 0.5f))
    this["shininess"]?.set(40f)
  }
  val trackMaterial = Material(texturedProgram).apply {
    this["colorTexture"]?.set(Texture2D(gl, "media/tile.png"))
    this["ks"]?.set(Vec3(0.1f, 0.1f, 0.1f))
    this["shininess"]?.set(10f)
  }
  val ballMaterial = Material(texturedProgram).apply {
    this["colorTexture"]?.set(Texture2D(gl, "media/ball.png"))
    this["ks"]?.set(Vec3(0.8f, 0.8f, 0.8f))
    this["shininess"]?.set(60f)
  }
  val heliMaterial = Material(texturedProgram).apply {
    this["colorTexture"]?.set(Texture2D(gl, "media/json/json/heli/heli.png"))
    this["ks"]?.set(Vec3(0.5f, 0.5f, 0.5f))
    this["shininess"]?.set(40f)
  }

  val fsEnvmapped = Shader(gl, GL.FRAGMENT_SHADER, "envmapped-fs.glsl")
  val envmappedProgram = Program(gl, vsTransform, fsEnvmapped)
  val envmappedMaterial = Material(envmappedProgram).apply {
    this["kr"]?.set(Vec3(0.33f, 0.33f, 0.2f))
    this["environment"]?.set(skyCubeTexture)
    //LABTODO: use rendered cube map
  }

  val gameObjects = ArrayList<GameObject>()
  val chassis = Avatar(*chevyChassisGeometries.map { Mesh(chevyMaterial, it) }.toTypedArray()).apply {
    position.set(0f, -5f, -60f)
  }
  // ground level: the bottom of the chevy's wheels
  val groundY = -11.5f
  val track = GameObject(Mesh(trackMaterial, TrackGeometry(gl, 40, 40, 20f))).apply {
    position.set(0f, groundY, 0f)
  }
  init {
    gameObjects.add(track)
    gameObjects.add(chassis)
    // rollers, resting on the track
    for ((x, z) in listOf(Pair(0f, -25f), Pair(12f, -18f), Pair(-12f, -18f),
                          Pair(45f, -60f), Pair(-45f, -60f), Pair(0f, -120f))) {
      gameObjects += Ball(5f, groundY, *sphereGeometries.map { Mesh(ballMaterial, it) }.toTypedArray()).apply {
        position.x = x
        position.z = z
      }
    }
    // wheel positions in chassis model space; tweak if they look off
    for ((x, z) in listOf(Pair(7f, 14f), Pair(-7f, 14f), Pair(7f, -11f), Pair(-7f, -11f))) {
      gameObjects += GameObject(*chevyWheelGeometries.map { Mesh(chevyMaterial, it) }.toTypedArray()).apply {
        position.set(x, -3.5f, z)
        if (x < 0f) yaw = PI.toFloat() // mirror so the rim faces outward
        parent = chassis
      }
    }
    val heli = GameObject(*heliBodyGeometries.map { Mesh(heliMaterial, it) }.toTypedArray()).apply {
      position.set(-45f, 0f, -90f)
      yaw = 0.8f
    }
    gameObjects.add(heli)
    // rotor hub positions in heli model space; tweak if they look off
    gameObjects += GameObject(*heliMainRotorGeometries.map { Mesh(heliMaterial, it) }.toTypedArray()).apply {
      position.set(0f, 15f, 4f)
      parent = heli
      move = object : GameObject.Motion(this) {
        override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
            interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
          gameObject.yaw += dt * 10f
          return true
        }
      }
    }
    gameObjects += GameObject(*heliTailRotorGeometries.map { Mesh(heliMaterial, it) }.toTypedArray()).apply {
      position.set(0.8f, 15f, -38f)
      parent = heli
      move = object : GameObject.Motion(this) {
        override operator fun invoke(dt : Float, t : Float, keysPressed : Set<String>,
            interactors : ArrayList<GameObject>, spawn : ArrayList<GameObject>) : Boolean {
          gameObject.pitch += dt * 20f
          return true
        }
      }
    }
    gameObjects.add(GameObject(*exhibitGeometries.map { Mesh(envmappedMaterial, it) }.toTypedArray()))
    val radius = 27f
    for (i in 0 until 5) {
      gameObjects += GameObject(*exhibitGeometries.map { Mesh(phongMaterial, it) }.toTypedArray()).apply{
        val phi = i.toFloat() / 5f * PI.toFloat() * 2f
        yaw = i.toFloat()
        position.set(
          cos(phi) * radius,
          -10f,
          sin(phi) * radius
        )
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
  val confusedCamera = PerspectiveCamera()  
  var focalDistance = 15f

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  init{
    gl.enable(GL.DEPTH_TEST)
  }


  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
    camera.setAspectRatio(canvas.width.toFloat() / canvas.height.toFloat())

    defaultFramebuffer = DefaultFramebuffer(canvas.width, canvas.height)
    // LABTODO: postProcFramebuffer, material
    // LABTODO: fameBufferCube
  }

  // LABTODO: cube cameras

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>, timeStamp : Double) {

    val timeAtThisFrame = Date().getTime() 
    // clamped so the stiff camera springs stay stable after a frame hiccup
    val dt = min((timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f, 0.05f)
    val t  = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    if(!keysPressed.isEmpty()){
      frameCount = 0
    }
    frameCount = frameCount+1

    // LABTODO: render to cube maps

    // clear the screen
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
    gameObjects.forEach { it.update() }
    heliCam.move(dt)

    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
    backgroundMesh.draw(camera)
    gameObjects.forEach { it.draw( camera, *lights ) }
  }
}
