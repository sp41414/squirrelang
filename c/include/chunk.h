#ifndef CHUNK_H
#define CHUNK_H

#include "common.h"
#include "value.h"

typedef enum {
  OP_CONSTANT,
  OP_RETURN,
} OpCode;

typedef struct {
  ValueArray constants;
  int capacity;
  int count;
  uint8_t *code;
  int *lines;
} Chunk;

void initChunk(Chunk *chunk);
void freeChunk(Chunk *chunk);
int addConstant(Chunk *chunk, Value value);
void writeChunk(Chunk *chunk, uint8_t byte, int line);

#endif
