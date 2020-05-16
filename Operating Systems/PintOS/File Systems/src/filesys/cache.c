/* -Proj3- New file for buffer cache used in filesystem. */

#include "filesys/cache.h"
#include "filesys/filesys.h"
#include "threads/thread.h"
#include "threads/malloc.h"
#include "threads/synch.h"
#include <debug.h>
#include <inttypes.h>
#include <round.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

struct cache_entry {
  block_sector_t sector_id;
  bool dirty;
  bool valid;              /* Set to 0 (invalid) when the cache hasn't been touched at all. */
  bool referenced;         /* Indicate if it's been used recently. Used for clock replacement policy. */
  struct lock entry_lock;
  char data[512];          /* Stores the corresponding sector's data. */
};

struct cache_entry cache_array[cache_max_size]; /* Representaion of the cache. */

struct lock cache_lock; /* Global lock. */

int clock_hand; /* Used for clock replacement policy. */

int total_count; /* For student testing. */

int hit_count;  /* For student testing. */


struct cache_entry* lookup_or_fetch_from_disk(struct block *block, block_sector_t sector_id, bool read);
struct cache_entry* look_up_sector_id(block_sector_t sector_id);
struct cache_entry* find_usable_entry(struct block *block);
void avdance_clock_hand(void);

/* Initializes the cache. */
void
cache_init(){
  lock_init(&cache_lock);
  clock_hand = 0;

  /* For student testing. */
  total_count = 0;
  hit_count = 0;

  lock_acquire(&cache_lock);
  // Generalize cache_max_size entries in the cache_array
  for (int i = 0; i < cache_max_size; ++i)
  {
    cache_array[i].valid = false;
    lock_init(&cache_array[i].entry_lock);
  }
  lock_release(&cache_lock);
}

/* Read sector SECTOR_ID to buffer from cache. */
void
cache_read (struct block *block, block_sector_t sector_id, void *buffer){
  // Look up the corresponding cache_entry with sector_id or fetch it from disk
  struct cache_entry * cache_entry_pt = lookup_or_fetch_from_disk(block,sector_id,true);
  // Copy the data from the entry to buffer, release the entry_lock
  memcpy(buffer,cache_entry_pt->data,BLOCK_SECTOR_SIZE);
  cache_entry_pt->referenced = true;
  lock_release(&cache_entry_pt->entry_lock);
}

/* Write buffer data (must be BLOCK_SECTOR_SIZE bytes) to sector SECTOR from cache. */
void
cache_write (struct block *block, block_sector_t sector_id, const void *buffer){
  // Look up the corresponding cache_entry with sector_id or fetch it from disk
  struct cache_entry * cache_entry_pt = lookup_or_fetch_from_disk(block,sector_id,false);

  // Copy the data from the buffer to entry, make it dirty and release the entry_lock
  memcpy(cache_entry_pt->data,buffer,BLOCK_SECTOR_SIZE);
  cache_entry_pt->dirty = true;
  cache_entry_pt->referenced = true;
  lock_release(&cache_entry_pt->entry_lock);

}

/* Byte-to-Byte layer of cache_read but reading starts at offset (from the sector) with size. */
void
cache_read_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size){
  // Look up the corresponding cache_entry with sector_id or fetch it from disk
  struct cache_entry * cache_entry_pt = lookup_or_fetch_from_disk(block,sector_id,true);
  // Copy the data from the entry to buffer, release the entry_lock
  memcpy(buffer, cache_entry_pt->data + offset, size);
  cache_entry_pt->referenced = true;
  lock_release(&cache_entry_pt->entry_lock);
}

/* Byte-to-Byte layer of cache_write but writing starts at offset (from the sector) with size. */
void
cache_write_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size){
   // Look up the corresponding cache_entry with sector_id or fetch it from disk
  struct cache_entry * cache_entry_pt = lookup_or_fetch_from_disk(block,sector_id,true);

  // Copy the data from the buffer to entry, make it dirty and release the entry_lock
  memcpy(cache_entry_pt->data + offset, buffer, size);
  cache_entry_pt->dirty = true;
  cache_entry_pt->referenced = true;
  lock_release(&cache_entry_pt->entry_lock);
}


/* Write all the dirty cache entry's data to the block device. */
void
cache_flush(struct block *block) {
  lock_acquire(&cache_lock);
  for (int i = 0; i < cache_max_size; ++i)
  {
    lock_acquire(&cache_array[i].entry_lock);
    if (cache_array[i].dirty && cache_array[i].valid){
      block_write(block,cache_array[i].sector_id,cache_array[i].data);
      cache_array[i].dirty = false;
    }
    lock_release(&cache_array[i].entry_lock);
  }
  lock_release(&cache_lock);
}

/* Helper functions below. */

/* Check to see if it is in the cache, and if so, use the cached data without going to disk.
   Otherwise, fetch the block from disk into the cache, evicting an older entry if necessary.
   Finally, store the **LOCKED** cache_entry in cache_entry_pt. */
struct cache_entry *
lookup_or_fetch_from_disk(struct block *block, block_sector_t sector_id, bool read){
  /* For student testing. */
  total_count++;

  lock_acquire(&cache_lock);

  struct cache_entry *cache_entry_pt = look_up_sector_id(sector_id);

  if (!cache_entry_pt){
    //sector is not in the cache, fetch a cache to be used

    cache_entry_pt = find_usable_entry (block);
    lock_release(&cache_lock);
    lock_acquire(&cache_entry_pt->entry_lock);
    //read data to cache_entry from block device
    cache_entry_pt->sector_id = sector_id;
    cache_entry_pt->dirty = false;
    cache_entry_pt->valid = true;
    //Whether need to fetch data from disk or not
    if (read){
      block_read(block,sector_id,cache_entry_pt->data);
    }
  }else {
    //sector is in the cache

    /* For student testing. */
    hit_count++;

    lock_release(&cache_lock);
    lock_acquire(&cache_entry_pt->entry_lock);
    if(cache_entry_pt->sector_id != sector_id){
      //sector is not the same,retry
      lock_release(&cache_entry_pt->entry_lock);
      cache_entry_pt = lookup_or_fetch_from_disk(block,sector_id,read);
    }
  }

  cache_entry_pt->referenced = true;
  return cache_entry_pt;
}

/* Look up corresponding cache_entry in the cache_list with sector_id, then return the pointer to it.
   Return NULL if can't find. It's caller's reponsibility to ensure concurrency of the list. */
struct cache_entry *
look_up_sector_id(block_sector_t sector_id){
  //must check if valid before returing
  for (int i = 0; i < cache_max_size; ++i)
  {
    if (cache_array[i].sector_id == sector_id && cache_array[i].valid){
      return &cache_array[i];
    }
  }
  return NULL;
}

/* Look for available cache_entry to use, return the pointer to it.
   Eviction and dirty entry flushing are taken care in this function.
   Caller should acquire cache_lock. */
struct cache_entry *
find_usable_entry(struct block *block){
    //start from clock hand, if not nvalid, means the entry is good to use
    //if valid, follow eviction process, keep looping until one entry available
    while (true){
      if (!cache_array[clock_hand].valid){
        // lock_init(&cache_array[clock_hand].entry_lock);
        int temp = clock_hand;
        avdance_clock_hand();
        return &cache_array[temp];
      }else{
        if (!lock_try_acquire(&cache_array[clock_hand].entry_lock)){
          //someone else is using this cache_entry, jump to next one
          avdance_clock_hand();
        }else {
          if (cache_array[clock_hand].referenced){
            //been used recently, set it to 0 and jump to next
            cache_array[clock_hand].referenced = false;
            lock_release(&cache_array[clock_hand].entry_lock);
            avdance_clock_hand();
          }else{
              //Good to use, but need to check if dirty
              if (cache_array[clock_hand].dirty){
                //Write back to block device
                block_write(block,cache_array[clock_hand].sector_id,cache_array[clock_hand].data);
                cache_array[clock_hand].dirty = false;
              }
              lock_release(&cache_array[clock_hand].entry_lock);
              int temp = clock_hand;
              avdance_clock_hand();
              return &cache_array[temp];
            }
        }
      }
    }
}

/* Advance clock hand. */
void
avdance_clock_hand(){
  clock_hand = (clock_hand + 1) % cache_max_size;
}

/* For student testing. */
void
cache_reset(){
  //Save the data to disk before reset
  cache_flush(fs_device);

  lock_acquire(&cache_lock);
  for (int i = 0; i < cache_max_size; ++i)
  {
    lock_acquire(&cache_array[i].entry_lock);
    //Make it cold cache
    cache_array[i].valid = false;
    lock_release(&cache_array[i].entry_lock);
  }
  lock_release(&cache_lock);
  clock_hand = 0;
}

void
hit_reset(){
  hit_count = 0;
  total_count = 0;
};

int
get_hit_rate(int dummy){
  return (hit_count*100)/total_count;
};
