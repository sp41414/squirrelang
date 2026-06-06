#include "chunk.h"
#include "debug.h"
int main(int argc, const char **argv) {
  Chunk chunk;
  initChunk(&chunk);

  int constant = addConstant(&chunk, 1.2);
  writeChunk(&chunk, OP_CONSTANT, 69420);
  writeChunk(&chunk, constant, 69420);

  writeChunk(&chunk, OP_RETURN, 69420);
  disassembleChunk(&chunk, "test chunk");
  freeChunk(&chunk);
  return 0;
}
