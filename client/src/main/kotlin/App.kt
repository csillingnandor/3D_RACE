import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.*

class App(val canvas : HTMLCanvasElement, val overlay : HTMLDivElement) {

  val gl = (canvas.getContext("webgl2", object{val antialias = false; val alpha = false}) ?: throw Error("Browser does not support WebGL2")) as WebGL2RenderingContext
  val scene = Scene(gl, overlay, canvas)
  init {
    resize()
  }

  fun resize() {
    canvas.width = canvas.clientWidth
    canvas.height = canvas.clientHeight
    scene.resize(gl, canvas)
  }

  val keysPressed = HashSet<String>()

  fun registerEventHandlers() {
    document.onkeydown = { event : KeyboardEvent ->
      keysPressed.add(keyNames[event.keyCode])
    }
    document.onkeyup = { event : KeyboardEvent ->
      keysPressed.remove(keyNames[event.keyCode])
    }
    window.onresize = { resize() }
    window.requestAnimationFrame { timeStamp -> update(timeStamp) }
  }

  fun update(timeStamp : Double) {
    scene.update(gl, keysPressed, timeStamp)
    window.requestAnimationFrame { nextTimeStamp -> update(nextTimeStamp) }
  }
}

fun main() {
  val canvas = document.getElementById("canvas") as HTMLCanvasElement
  val overlay = document.getElementById("overlay") as HTMLDivElement
  try {
    App(canvas, overlay).registerEventHandlers()
  } catch(e : Error) {
    console.error(e.message)
  }
}
