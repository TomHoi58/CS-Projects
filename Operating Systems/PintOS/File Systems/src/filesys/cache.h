/* -Proj3- New file for buffer cache used in filesystem. */
#ifndef FILESYS_CACHE_H
#define FILESYS_CACHE_H

#include <stdbool.h>
#include "filesys/off_t.h"
#include "devices/block.h"
  



#define cache_max_size 64

void cache_init (void);
void cache_read (struct block *block, block_sector_t sector_id, void *buffer);
void cache_write (struct block *block, block_sector_t sector_id, const void *buffer);
void cache_read_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size);
void cache_write_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size);
void cache_flush (struct block *block);

/* For student testing. */
void cache_reset(void);
void hit_reset(void);
int get_hit_rate(int dummy);

#endif /* filesys/cache.h */
