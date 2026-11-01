class TexturedQuadGeometry(gl : WebGL2RenderingContext) : IndexedGeometry(gl, listOf(
    VertexAttribute(arrayOf(
      -1.0f, -1.0f, 0.5f,
      -1.0f,  1.0f, 0.5f,
       1.0f, -1.0f, 0.5f,
       1.0f,  1.0f, 0.5f), 3),
    VertexAttribute(arrayOf(
      0.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f), 3),
    VertexAttribute(arrayOf(
      0.0f, 1.0f,
      0.0f, 0.0f,
      1.0f, 1.0f,
      1.0f, 0.0f), 2)),
  arrayOf<Short>(0, 1, 2, 1, 2, 3))
