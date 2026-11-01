class SubmeshGeometry(gl : WebGL2RenderingContext, jsonMesh : JsonMesh) : IndexedGeometry(gl, listOf(
    VertexAttribute(jsonMesh.vertices, 3),
    VertexAttribute(jsonMesh.normals, 3),
    VertexAttribute(jsonMesh.texturecoords[0], 2),
    VertexAttribute(jsonMesh.tangents, 3),
    VertexAttribute(jsonMesh.bitangents, 3)),
  jsonMesh.faces.flatten().toTypedArray())
