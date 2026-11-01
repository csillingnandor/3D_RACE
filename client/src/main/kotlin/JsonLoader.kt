import org.w3c.xhr.XMLHttpRequest
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import vision.gears.webglmath.Geometry

@Serializable
data class JsonMesh(
  val vertices : Array<Float>,
  val normals : Array<Float>,
  val tangents : Array<Float>,
  val bitangents : Array<Float>,
  val texturecoords : Array<Array<Float>>,
  val faces : Array<Array<Short>>)

@Serializable
data class JsonModel(
  val meshes : Array<JsonMesh>)

class JsonLoader {
  val json = Json { ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true }

  fun loadGeometries(gl : WebGL2RenderingContext, jsonModelFileUrl : String) : List<Geometry> {
    val request = XMLHttpRequest()
    request.overrideMimeType("application/json")
    request.open("GET", jsonModelFileUrl, false)
    var geometries : List<Geometry>? = null
    request.onloadend = {
      val jsonModel = json.decodeFromString(JsonModel.serializer(), request.responseText)
      geometries = jsonModel.meshes.map { SubmeshGeometry(gl, it) }
      Unit
    }
    request.send()
    return geometries ?: throw Error("Geometries never loaded.")
  }
}
