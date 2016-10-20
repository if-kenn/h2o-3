package water.fvec;

import water.Futures;
import water.Iced;
import water.parser.BufferedString;
import water.util.ArrayUtils;

import java.util.Arrays;

/**
 * Created by tomas on 10/5/16.
 */
public class ChunkAry<C extends Chunk> extends Iced {
  public final Vec _vec;
  public final long _start;
  public final int _len; // numrows in this chunk
  public final int _numCols;
  public final int _cidx;
  ArrayUtils.IntAry _changedCols;
  C [] _cs;
  int [] _ids;

  public ChunkAry(Vec v,int cidx, C [] cs, int [] ids){
    _vec = v;
    _cidx = cidx;
    _start = _vec.chunk2StartElem(cidx);
    _len = (int)(_vec.chunk2StartElem(cidx+1) - _start);
    _numCols = v.numCols();
    _cs = cs;
    _ids = ids;
  }

  public int byteSize(){
    int s = 0;
    for(Chunk c:_cs)
      s += c.byteSize();
    return s;
  }

  public double min(int c){return _cs[c].min();}
  public double min(){return min(0);}
  public double max(int c){return _cs[c].max();}
  public double max(){return max(0);}

  public long start(){return _vec.chunk2StartElem(_cidx);}

  public void close(){close(new Futures()).blockForPending();}
  public Futures close(Futures fs){
    if(_changedCols == null || _changedCols.size() == 0)
      return fs;
    for(int i = 0; i < _cs.length; ++i)
      _cs[i] = (C)_cs[i].compress();
    return _vec.closeChunk(_cidx,this,fs);
  }



  private boolean isSparse(){return _ids != null;}
  public Chunk getChunk(int c){return _cs[c];}
  public Chunk[] getChunks(){return _cs;}

  private void setWrite(int j){
    if(_changedCols == null) _changedCols = new ArrayUtils.IntAry();
    _changedCols.add(j);
  }

  public final double set(int i, double d){ set(i,0,d); return d; }
  public final double set(int i, int j, double d){
    setWrite(j);
    if(!_cs[j].set_impl(i,d)){
      _cs[j] = (C)_cs[j].inflate_impl(new NewChunk());
      _cs[j].set_impl(i,d);
    }
    return d;
  }

  public long set(int i, int j, long l){
    setWrite(j);
    if(!_cs[j].set_impl(i,l)){
      _cs[j] = (C)_cs[j].inflate_impl(new NewChunk());
      _cs[j].set_impl(i,l);
    }
    return l;
  }

  public boolean hasNA(){return hasNA(0);}
  public boolean hasNA(int c){
    if(_ids != null){
      c = Arrays.binarySearch(_ids,c);
      if(c < 0) return false;
    }
    return _cs[c].hasNA();
  }

  public final double atd(int i){ return atd(i,0);}
  public final double atd(int i, int j){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) return 0;
    }
    return _cs[j].atd_impl(i);
  }

  public int chunkRelativeOffset(long globalRowId){
    long start = start();
    long x = globalRowId - (start>0 ? start : 0);
    if( 0 <= x && x < _len) return(int)x;
    throw new ArrayIndexOutOfBoundsException(""+start+" <= "+globalRowId+" < "+(start+ _len));
  }

  public final long at16l(int i){ return at16l(i,0);}
  public final long at16l(int i, int j){
    if(j < 0 || j > _numCols)
      throw new ArrayIndexOutOfBoundsException(j);
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) throw new IllegalArgumentException("not a UUId chunk"); // UUID chunks should not be 0-sparse
    }
    return _cs[j].at16l_impl(i);
  }
  public final long at16h(int i){ return at16h(i,0);}
  public final long at16h(int i, int j){
    if(j < 0 || j > _numCols)
      throw new ArrayIndexOutOfBoundsException(j);
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) throw new IllegalArgumentException("not a UUId chunk"); // UUID chunks should not be 0-sparse
    }
    return _cs[j].at16h_impl(i);
  }


  public boolean isNA(int i){ return isNA(i,0);}
  public boolean isNA(int i, int j){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) return false;
    }
    return _cs[j].isNA_impl(i);
  }

  public long at8(int i){ return at8(i,0);}
  public long at8(int i, int j){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) return 0;
    }
    return _cs[j].at8_impl(i);
  }

  public int at4(int i){ return at4(i,0);}
  public int at4(int i, int j){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) return 0;
    }
    return _cs[j].at4_impl(i);
  }

  public BufferedString atStr(BufferedString str,int i){
    return atStr(str,i,0);
  }
  public BufferedString atStr(BufferedString str,int i, int j){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) throw new ArrayIndexOutOfBoundsException(j);
    }
    return _cs[j].atStr_impl(str,i);
  }

  public String set(int i, int j, String str){
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) throw new ArrayIndexOutOfBoundsException(j);
    }
    return _cs[j].set(i,str);
  }

  private Chunk getOrMakeSparseChunk(int j){
    int j2 = Arrays.binarySearch(_ids,j);
    if(j2 > j) return _cs[j2];
    j2 = -j2 - 1;
    int n = _cs.length;
    _cs = Arrays.copyOf(_cs,n+1);
    _ids = Arrays.copyOf(_ids,n+1);
    for(int i = n; i > j2; --i) {
      _cs[i + 1] = _cs[i];
      _ids[i+1] = _ids[i+1];
    }
    _ids[j2] = j;
    return _cs[j2] = (C)new C0DChunk(0, _len);
  }

  public void setNA(int i, int j){
    Chunk c = _ids != null?getOrMakeSparseChunk(j):_cs[j];
    c.setNA_impl(i);
  }

  public int[] changedCols() {
    return _changedCols == null?new int[0]:_changedCols.toArray();
  }

  public boolean hasFloat(int c) {
    if(_ids != null){
      int j = Arrays.binarySearch(_ids,c);
      return j >= 0 && _cs[j].hasFloat();
    }
    return _cs[c].hasFloat();
  }
  public boolean hasFloat() {
    return hasFloat(0);
  }


  public int sparseLenZero(int c) {
    if(_ids != null){
      int j = Arrays.binarySearch(_ids,c);
      return j >= 0?_cs[j].sparseLenZero():0;
    }
    return _cs[c].sparseLenZero();
  }
  public int sparseLenZero(){return sparseLenZero(0);}


  public boolean isSparseNA(int c){
    if(_ids != null){
      int j = Arrays.binarySearch(_ids,c);
      return j >= 0?_cs[j].isSparseNA():false;
    }
    return _cs[c].isSparseZero();
  }
  public boolean isSparseNA(){return isSparseNA(0);}
  public boolean isSparseZero(int c){
    if(_ids != null){
      int j = Arrays.binarySearch(_ids,c);
      return j >= 0?_cs[j].isSparseZero():true;
    }
    return _cs[c].isSparseZero();
  }
  public boolean isSparseZero(){return isSparseZero(0);}

  public int nextNZ(int i) { return nextNZ(i,0);}
  public int nextNZ(int i, int j) {
    if(_ids != null){
      j = Arrays.binarySearch(_ids,j);
      if(j < 0) return _len;
    }
    return _cs[j].nextNZ(i);
  }

  public void add2Chunk(int srcCol, NewChunkAry nchks, int dstCol, int... rows) {
    _cs[srcCol].add2Chunk(nchks,dstCol,rows);
  }
}
