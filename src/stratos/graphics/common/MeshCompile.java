


package stratos.graphics.common;
import stratos.util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.*;



public class MeshCompile {
  
  
  final static int DEFAULT_QUAD_INDEX_ORDER[] = {
    0, 2, 1, 1, 2, 3
  };
  
  final public int
    vertexSize,
    pieceSize,
    maxPieces,
    maxSize;
  
  final Mesh compiled;
  final float verts[];
  final short indices[];
  private int marker = 0;
  
  
  public MeshCompile(
    int vertexSize, boolean useQuads, int maxPieces,
    int quadIndexOrder[],
    VertexAttribute... attributes
  ) {
    this.vertexSize = vertexSize;
    this.pieceSize = useQuads ? (vertexSize * 4) : (vertexSize * 3);
    this.maxPieces = maxPieces;
    this.maxSize = pieceSize * maxPieces;
    
    verts = new float[maxSize];
    indices = new short[maxPieces * (useQuads ? 6 : 3)];
    compiled = new Mesh(
      Mesh.VertexDataType.VertexArray,
      false,
      verts.length / vertexSize, indices.length,
      attributes
    );
    
    //  Next, we need to fill the index array.
    if (useQuads) {
      if (quadIndexOrder == null) quadIndexOrder = DEFAULT_QUAD_INDEX_ORDER;
      for (int p = 0, i = 0; p < maxPieces; p += 6) {
        indices[i++] = (short) (p + quadIndexOrder[0]);
        indices[i++] = (short) (p + quadIndexOrder[1]);
        indices[i++] = (short) (p + quadIndexOrder[2]);
        indices[i++] = (short) (p + quadIndexOrder[3]);
        indices[i++] = (short) (p + quadIndexOrder[4]);
        indices[i++] = (short) (p + quadIndexOrder[5]);
      }
    }
    else for (int i = 0; i < indices.length; i++) {
      indices[i] = (short) i;
    }
    
    compiled.setVertices(verts);
    compiled.setIndices(indices);
  }
  
  
  public void appendVertex(float buffer[]) {
    if (buffer == null || buffer.length != vertexSize) {
      I.complain("Incorrect buffer size!");
    }
    System.arraycopy(buffer, 0, verts, marker, vertexSize);
    marker += vertexSize;
  }
  
  
  public void appendPiece(float buffer[]) {
    if (buffer == null || buffer.length != pieceSize) {
      I.complain("Incorrect buffer size!");
    }
    System.arraycopy(buffer, 0, verts, marker, pieceSize);
    marker += pieceSize;
  }
  
  
  public boolean meshFull() {
    return marker >= maxSize;
  }
  
  
  public void renderWithShader(ShaderProgram shading, boolean reset) {
    if (marker <= 0) return;
    I.add("\nRendering, marker size: "+marker+", vertices: "+(marker / vertexSize));
    //for (int f = 0; f < marker; f++) I.add(", "+verts[f]);
    compiled.render(shading, GL11.GL_TRIANGLES, 0, marker / vertexSize);
    if (reset) reset();
  }
  
  
  public boolean reset() {
    if (marker == 0) return false;
    marker = 0;
    return true;
  }
}





