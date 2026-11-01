class TrackGeometry(
  gl : WebGL2RenderingContext,
  tilesX : Int,
  tilesZ : Int,
  tileSize : Float,
  isMissing : (Int, Int) -> Boolean) : IndexedGeometry(gl, tiles(tilesX, tilesZ, tileSize, isMissing)) {

  companion object {
    private fun tiles(tilesX : Int, tilesZ : Int, tileSize : Float, isMissing : (Int, Int) -> Boolean) : GeometryData {
      val data = GeometryData()
      for (i in 0 until tilesX) {
        for (j in 0 until tilesZ) {
          if (isMissing(i, j)) continue
          val x0 = (i - tilesX * 0.5f) * tileSize
          val z0 = (j - tilesZ * 0.5f) * tileSize
          val corners = listOf(Pair(0f, 0f), Pair(1f, 0f), Pair(0f, 1f), Pair(1f, 1f)).map { (u, v) ->
            data.vertex(x0 + u * tileSize, 0f, z0 + v * tileSize, 0f, 1f, 0f, u, v)
          }
          data.triangle(corners[0], corners[2], corners[1])
          data.triangle(corners[1], corners[2], corners[3])
        }
      }
      return data
    }
  }
}
