#include "filesys/inode.h"
#include <debug.h>
#include <round.h>
#include <string.h>
#include "filesys/filesys.h"
#include "filesys/free-map.h"
#include "threads/malloc.h"
#include "filesys/cache.h"

/* -Proj3- Use buffer caceh instead to read/write data from block device.
   Create extended file system. */

/* Identifies an inode. */
#define INODE_MAGIC 0x494e4f44
/* Number of direct pointers in inode. */
#define DIRECT_POINTERS 123
/* Number of pointers in a block. */
#define POINTERS_IN_BLOCK 128
/* Number of data blocks(512 bytes/block) DIRECT_POINTERS can cover. */
#define DIRECT_POINTERS_LIMIT DIRECT_POINTERS
/* Number of data blocks(512 bytes/block) DIRECT_POINTERS + INDIRECT_POINTER  can cover. */
#define INDIRECT_POINTERS_LIMIT (DIRECT_POINTERS_LIMIT + POINTERS_IN_BLOCK)
/* Number of data blocks(512 bytes/block) DIRECT_POINTERS + INDIRECT_POINTER + DB_INDIRECT_POINTERS can cover. */
#define DB_INDIRECT_POINTERS_LIMIT (INDIRECT_POINTERS_LIMIT + POINTERS_IN_BLOCK * POINTERS_IN_BLOCK )

/* Helper functions declaration below. */
struct inode_disk* get_inode_disk (struct inode *inode);

bool inode_resize (struct inode_disk *disk_inode, off_t length);
bool allocate_indirect (block_sector_t *sector_id_pt, size_t sectors);
bool allocate_db_indirect (block_sector_t *sector_id_pt, size_t sectors);
void inode_deallocate (struct inode *inode);

/* Synchronization for free-map. */
struct lock freemap_lock;
/* Synchronization for open_nodes list. */
struct lock open_inodes_lock;

/* On-disk inode.
   Must be exactly BLOCK_SECTOR_SIZE bytes long. */
struct inode_disk
  {
    block_sector_t direct[DIRECT_POINTERS];
    block_sector_t indirect;
    block_sector_t db_indirect;
    off_t length;                       /* File size in bytes. */
    bool dir;                           /* Is directory or not. */
    unsigned magic;                     /* Magic number. */
  };

/* Indirect block struct which store pointers. */
struct indirect_block
{
  block_sector_t pointers[POINTERS_IN_BLOCK];
};

/* Returns the number of sectors to allocate for an inode SIZE
   bytes long. */
static inline size_t
bytes_to_sectors (off_t size)
{
  return DIV_ROUND_UP (size, BLOCK_SECTOR_SIZE);
}


/* Returns the block device sector that contains byte offset POS
   within INODE.
   Returns -1 if INODE does not contain data for a byte at offset
   POS. */
static block_sector_t
byte_to_sector (const struct inode *inode, off_t pos)
{
  ASSERT (inode != NULL);

  struct inode_disk * inode_disk_pt = get_inode_disk(inode);

  if(pos >= inode_disk_pt->length){
    free(inode_disk_pt);
    return -1;
  }

  off_t sector_th = pos / BLOCK_SECTOR_SIZE;

  block_sector_t result; 

  struct indirect_block indirect_block_struct;

  if (sector_th < DIRECT_POINTERS){
    //direct pointers can cover up
    result = inode_disk_pt->direct[sector_th];
  }else if (sector_th < DIRECT_POINTERS + POINTERS_IN_BLOCK){
    //indirect pointers can cover up 
    //index of the direct pointer in the indirect struct
    off_t direct_th_in_indirect = sector_th - DIRECT_POINTERS;
    //retrieve the indirect struct
    cache_read(fs_device, inode_disk_pt->indirect, &indirect_block_struct);
    //Get the sector_id from the indirect struct
    result = indirect_block_struct.pointers[direct_th_in_indirect];
  }else {
    //db indirect pointers can cover up 
    //index of the indirect pointer in the db indirect struct
    off_t indirect_th_in_db_indirect = (sector_th - DIRECT_POINTERS - POINTERS_IN_BLOCK) / POINTERS_IN_BLOCK;
    //retrieve the db indirect struct
    cache_read(fs_device, inode_disk_pt->db_indirect, &indirect_block_struct);
    //index of the direct pointer in the indirect struct
    off_t direct_th_in_indirect = sector_th - (indirect_th_in_db_indirect * POINTERS_IN_BLOCK);
    //retrieve the indirect struct
    cache_read(fs_device, indirect_block_struct.pointers[indirect_th_in_db_indirect], &indirect_block_struct);
    //Get the sector_id from the indirect struct
    result = indirect_block_struct.pointers[direct_th_in_indirect];
  }

  free(inode_disk_pt);

  return result;
}

/* List of open inodes, so that opening a single inode twice
   returns the same `struct inode'. */
static struct list open_inodes;

/* Initializes the inode module. */
void
inode_init (void)
{
  list_init (&open_inodes);
  lock_init (&freemap_lock);
  lock_init (&open_inodes_lock);
}

/* Initializes an inode with LENGTH bytes of data and
   writes the new inode to sector SECTOR on the file system
   device.
   Returns true if successful.
   Returns false if memory or disk allocation fails. */
bool
inode_create (block_sector_t sector, off_t length, bool dir_or_not)
{
  struct inode_disk *disk_inode = NULL;
  bool success = false;

  ASSERT (length >= 0);

  /* If this assertion fails, the inode structure is not exactly
     one sector in size, and you should fix that. */
  ASSERT (sizeof *disk_inode == BLOCK_SECTOR_SIZE);

  if (length >= DB_INDIRECT_POINTERS_LIMIT * BLOCK_SECTOR_SIZE){
    //File too large
    return false;
  }

  disk_inode = calloc (1, sizeof *disk_inode);
  if (disk_inode != NULL)
    {
      success = inode_resize(disk_inode, length);
      disk_inode->dir = dir_or_not;
      disk_inode->magic = INODE_MAGIC;
      if (success){
        cache_write(fs_device,sector,disk_inode);
      }
      
      free (disk_inode);
    }
  return success;
}

/* Reads an inode from SECTOR
   and returns a `struct inode' that contains it.
   Returns a null pointer if memory allocation fails. */
struct inode *
inode_open (block_sector_t sector)
{
  struct list_elem *e;
  struct inode *inode;

  /* Check whether this inode is already open. */
  for (e = list_begin (&open_inodes); e != list_end (&open_inodes);
       e = list_next (e))
    {
      inode = list_entry (e, struct inode, elem);
      if (inode->sector == sector)
        {
          inode_reopen (inode);
          return inode;
        }
    }
  /* Allocate memory. */
  inode = malloc (sizeof *inode);
  if (inode == NULL)
    return NULL;

  /* Initialize. */
  lock_acquire (&open_inodes_lock);
  list_push_front (&open_inodes, &inode->elem);
  lock_release (&open_inodes_lock);
  inode->sector = sector;
  inode->open_cnt = 1;
  inode->deny_write_cnt = 0;
  inode->removed = false;
  lock_init (&inode->file_growth_lock);
  return inode;
}

/* Reopens and returns INODE. */
struct inode *
inode_reopen (struct inode *inode)
{
  if (inode != NULL)
    inode->open_cnt++;
  return inode;
}

/* Returns INODE's inode number. */
block_sector_t
inode_get_inumber (const struct inode *inode)
{
  return inode->sector;
}

/* Closes INODE and writes it to disk.
   If this was the last reference to INODE, frees its memory.
   If INODE was also a removed inode, frees its blocks. */
void
inode_close (struct inode *inode)
{
  /* Ignore null pointer. */
  if (inode == NULL)
    return;

  /* Release resources if this was the last opener. */
  if (--inode->open_cnt == 0)
    {
      /* Remove from inode list and release lock. */
      lock_acquire (&open_inodes_lock);
      list_remove (&inode->elem);
      lock_release (&open_inodes_lock);
      /* Deallocate blocks if removed. */
      if (inode->removed)
        {
          lock_acquire (&freemap_lock);
          free_map_release (inode->sector, 1);
          lock_release (&freemap_lock);
          inode_deallocate (inode);
        }

      free (inode);
    }
}

/* Marks INODE to be deleted when it is closed by the last caller who
   has it open. */
void
inode_remove (struct inode *inode)
{
  ASSERT (inode != NULL);
  inode->removed = true;
}

/* Reads SIZE bytes from INODE into BUFFER, starting at position OFFSET.
   Returns the number of bytes actually read, which may be less
   than SIZE if an error occurs or end of file is reached. */
off_t
inode_read_at (struct inode *inode, void *buffer_, off_t size, off_t offset)
{
  uint8_t *buffer = buffer_;
  off_t bytes_read = 0;

  while (size > 0)
    {
      /* Disk sector to read, starting byte offset within sector. */
      block_sector_t sector_idx = byte_to_sector (inode, offset);
      int sector_ofs = offset % BLOCK_SECTOR_SIZE;

      /* Bytes left in inode, bytes left in sector, lesser of the two. */
      off_t inode_left = inode_length (inode) - offset;
      int sector_left = BLOCK_SECTOR_SIZE - sector_ofs;
      int min_left = inode_left < sector_left ? inode_left : sector_left;

      /* Number of bytes to actually copy out of this sector. */
      int chunk_size = size < min_left ? size : min_left;
      if (chunk_size <= 0)
        break;

      if (sector_ofs == 0 && chunk_size == BLOCK_SECTOR_SIZE)
        {
          /* Read full sector directly into caller's buffer. */
          cache_read (fs_device, sector_idx, buffer + bytes_read);
        }
      else
        {
          //No more bounce needed
          cache_read_at (fs_device, sector_idx, buffer + bytes_read, sector_ofs, chunk_size);
        }

      /* Advance. */
      size -= chunk_size;
      offset += chunk_size;
      bytes_read += chunk_size;
    }

  return bytes_read;
}

/* Writes SIZE bytes from BUFFER into INODE, starting at OFFSET.
   Returns the number of bytes actually written, which may be
   less than SIZE if end of file is reached or an error occurs.
   (Normally a write at end of file would extend the inode, but
   growth is not yet implemented.) */
off_t
inode_write_at (struct inode *inode, const void *buffer_, off_t size,
                off_t offset)
{
  const uint8_t *buffer = buffer_;
  off_t bytes_written = 0;


  if (inode->deny_write_cnt)
    return 0;

  //If the write make the file grow too big, reject
  if (offset + size > DB_INDIRECT_POINTERS_LIMIT * BLOCK_SECTOR_SIZE){
    return 0;
  }

  //See if we need to grow the file
  if (byte_to_sector(inode, offset + size - 1) == -1){
    //Grow the file
    struct inode_disk * inode_disk_pt = get_inode_disk(inode);

    //Double checking lock
    lock_acquire (&inode->file_growth_lock);
    if (byte_to_sector(inode, offset + size - 1) == -1){
      if (!inode_resize(inode_disk_pt, offset + size)){
        lock_release (&inode->file_growth_lock);
        free(inode_disk_pt);
        return 0;
      }
    }
    lock_release (&inode->file_growth_lock);

    //write back the inode to disk
    cache_write(fs_device,inode->sector, inode_disk_pt);

    free(inode_disk_pt);
  }

  while (size > 0)
    {
      /* Sector to write, starting byte offset within sector. */
      block_sector_t sector_idx = byte_to_sector (inode, offset);
      int sector_ofs = offset % BLOCK_SECTOR_SIZE;

      /* Bytes left in inode, bytes left in sector, lesser of the two. */
      off_t inode_left = inode_length (inode) - offset;
      int sector_left = BLOCK_SECTOR_SIZE - sector_ofs;
      int min_left = inode_left < sector_left ? inode_left : sector_left;

      /* Number of bytes to actually write into this sector. */
      int chunk_size = size < min_left ? size : min_left;
      if (chunk_size <= 0)
        break;

      if (sector_ofs == 0 && chunk_size == BLOCK_SECTOR_SIZE)
        {
          /* Write full sector directly to disk. */
          cache_write (fs_device, sector_idx, buffer + bytes_written);
        }
      else
        {
          //No more bounce needed
          cache_write_at (fs_device, sector_idx, buffer + bytes_written, sector_ofs, chunk_size);
        }

      /* Advance. */
      size -= chunk_size;
      offset += chunk_size;
      bytes_written += chunk_size;
    }

  return bytes_written;
}

/* Disables writes to INODE.
   May be called at most once per inode opener. */
void
inode_deny_write (struct inode *inode)
{
  inode->deny_write_cnt++;
  ASSERT (inode->deny_write_cnt <= inode->open_cnt);
}

/* Re-enables writes to INODE.
   Must be called once by each inode opener who has called
   inode_deny_write() on the inode, before closing the inode. */
void
inode_allow_write (struct inode *inode)
{
  ASSERT (inode->deny_write_cnt > 0);
  ASSERT (inode->deny_write_cnt <= inode->open_cnt);
  inode->deny_write_cnt--;
}

/* Returns the length, in bytes, of INODE's data. */
off_t
inode_length (const struct inode *inode)
{
  struct inode_disk * inode_disk_pt = get_inode_disk(inode);
  off_t result = inode_disk_pt->length;
  free(inode_disk_pt);
  return result;
}

/* Functions to support directories. */

/* Set the inode to be directory or not. */
void
inode_set_dir(struct inode * inode, bool is_dir_or_not){
  struct inode_disk* inode_disk_pt = get_inode_disk(inode);
  inode_disk_pt->dir = is_dir_or_not;
  cache_write(fs_device, inode->sector, inode_disk_pt);
  free(inode_disk_pt);
}

/* See if the inode is directory or not */
bool
inode_dir(struct inode * inode){
  struct inode_disk* inode_disk_pt = get_inode_disk(inode);
  bool result = inode_disk_pt->dir;
  free(inode_disk_pt);
  return result;
}

/* Helper functions declaration below. */

/* Get the inode_disk out of in memory inode. Caller's responsibility to free the pointer. */
struct inode_disk* 
get_inode_disk (struct inode *inode)
{
  struct inode_disk * inode_disk_pt = malloc(sizeof (struct inode_disk));
  cache_read (fs_device, inode->sector, inode_disk_pt);
  return inode_disk_pt;
}

/* Expand the size of disk_inode. Caller's responsibility to make sure the length > original length && length < max file length
   and ensure synchronization of file growth. */
bool 
inode_resize (struct inode_disk *inode_disk_pt, off_t length){
  //Prepare zero filled buffer
  char zeros[BLOCK_SECTOR_SIZE];
  memset(zeros,0,BLOCK_SECTOR_SIZE);

  //Calculate new number of blocks needed
  size_t new_sectors = bytes_to_sectors(length);

  //Get number of direct sectors needed before using indirect pointer
  size_t direct_sectors = new_sectors < DIRECT_POINTERS ? new_sectors : DIRECT_POINTERS;

  new_sectors = new_sectors - direct_sectors;
  //Allocate number of direct sectors
  for (size_t i = 0; i < direct_sectors; ++i)
  {
    if (inode_disk_pt->direct[i] == NULL){
      lock_acquire (&freemap_lock);
      if (free_map_allocate(1,&(inode_disk_pt->direct[i]))){
        lock_release (&freemap_lock);
        cache_write(fs_device,inode_disk_pt->direct[i],zeros);
      }else {
        lock_release (&freemap_lock);
        return false;
      }
    }
  }
 
  //Check if we are done
  if (new_sectors == 0){
    inode_disk_pt->length = length;
    return true;
  }

  //Get number of indirect sectors needed before using db indirect pointer
  size_t indirect_sectors = new_sectors < POINTERS_IN_BLOCK ? new_sectors : POINTERS_IN_BLOCK;

  new_sectors = new_sectors - indirect_sectors;
  //Allocate number of indirect sectors
  if (!allocate_indirect(&inode_disk_pt->indirect, indirect_sectors)){
    return false;
  }
 
  //Check if we are done
  if (new_sectors == 0){
    inode_disk_pt->length = length;
    return true;
  }


  //Get number of db indirect sectors needed 
  size_t db_indirect_sectors = new_sectors;
  //Allocate number of  db direct sectors
  if (!allocate_db_indirect(&inode_disk_pt->db_indirect,db_indirect_sectors)){
    return false;
  }

  inode_disk_pt->length = length;
  return true;
}

/* Allocate indirect pointer of a disk_inode and its direct pointers given sectors if necessary. */
bool
allocate_indirect (block_sector_t *sector_id_pt, size_t sectors){
  //Prepare zero filled buffer
  char zeros[BLOCK_SECTOR_SIZE];
  memset(zeros,0,BLOCK_SECTOR_SIZE);

  //Allocate indirect pointer if it hasn't been
  if (*sector_id_pt == NULL){
    lock_acquire (&freemap_lock);
    if (free_map_allocate(1,sector_id_pt)){
      lock_release (&freemap_lock);
      cache_write(fs_device,*sector_id_pt,zeros);
    }else {
      lock_release (&freemap_lock);
      return false;
    }
  }

  //Bring in the indirect block from disk
  struct indirect_block *indirect_block_pt = malloc(sizeof (struct indirect_block));
  cache_read(fs_device,*sector_id_pt, indirect_block_pt);

  //Allocate the direct pointers if they haven't been
  for (size_t i = 0; i < sectors; ++i)
  {
    if (indirect_block_pt->pointers[i] == NULL){
      lock_acquire (&freemap_lock);
      if (free_map_allocate(1,&(indirect_block_pt->pointers[i]))){
        lock_release (&freemap_lock);
        cache_write(fs_device,indirect_block_pt->pointers[i],zeros);
      }else {
        lock_release (&freemap_lock);
        return false;
      }
    } 
  }

  //Save the indirect back to disk
  cache_write(fs_device, *sector_id_pt, indirect_block_pt);
  free(indirect_block_pt);
  return true;
};

/* Allocate db indirect pointer of a disk_inode and its indirect pointers  and direct pointers given sectors if necessary. */
bool 
allocate_db_indirect (block_sector_t *sector_id_pt, size_t sectors){
   //Prepare zero filled buffer
  char zeros[BLOCK_SECTOR_SIZE];
  memset(zeros,0,BLOCK_SECTOR_SIZE);

  //Allocate db indirect pointer if it hasn't been
  if (*sector_id_pt == NULL){
    lock_acquire (&freemap_lock);
    if (free_map_allocate(1,sector_id_pt)){
      lock_release (&freemap_lock);
      cache_write(fs_device,*sector_id_pt,zeros);
    }else {
      lock_release (&freemap_lock);
      return false;
    }
  }

  //Bring in the indirect block from disk
  struct indirect_block *indirect_block_pt = malloc(sizeof (struct indirect_block));
  cache_read(fs_device,*sector_id_pt, indirect_block_pt);

  //Allocate the indirect pointers if they haven't been
  for (size_t i = 0; i < DIV_ROUND_UP(sectors,POINTERS_IN_BLOCK); ++i)
  {
    if (sectors < POINTERS_IN_BLOCK){
      if (allocate_indirect(&(indirect_block_pt->pointers[i]),sectors)){
        sectors = 0;
      }else {
        return false;
      }
    }else {
      if (allocate_indirect(&(indirect_block_pt->pointers[i]),POINTERS_IN_BLOCK)){
        sectors = sectors - POINTERS_IN_BLOCK;
      }else {
        return false;
      }
    }
  }

  //Save the db indirect back to disk
  cache_write(fs_device, *sector_id_pt, indirect_block_pt);
  free(indirect_block_pt);
  return true;

};


//Deallocate everything stored in disk inode of inode
void
inode_deallocate (struct inode *inode_pt){
  //Get the disk_inode
  struct inode_disk *inode_disk_pt = get_inode_disk(inode_pt);

  //Get sectors needed to be deallocated
  size_t sectors = bytes_to_sectors(inode_disk_pt->length);

  /* Deallocate direct sectors */
  //Get number of direct sectors needed before deallocating indirect pointer
  size_t direct_sectors = sectors < DIRECT_POINTERS ? sectors : DIRECT_POINTERS;
  
  sectors = sectors - direct_sectors;
  //Deallocate the sectors one by one
  for (size_t i = 0; i < direct_sectors; ++i)
  { 
    lock_acquire (&freemap_lock);
    free_map_release(inode_disk_pt->direct[i],1);
    lock_release (&freemap_lock);
  }

  //Check if we are done
  if (sectors == 0){
    free(inode_disk_pt);
    return;
  }

  /* Deallocate indirect sectors */
  //Get number of indirect sectors needed before deallocating db indirect pointer
  size_t indirect_sectors = sectors < POINTERS_IN_BLOCK ? sectors : POINTERS_IN_BLOCK;
  
  sectors = sectors - indirect_sectors;
  //Retrieve indirect_block struct
  struct indirect_block * indirect_block_pt = malloc(sizeof (struct indirect_block));
  cache_read(fs_device,inode_disk_pt->indirect,indirect_block_pt);

  //Deallocate the sectors one by one
  for (size_t i = 0; i < indirect_sectors; ++i)
  {
    lock_acquire (&freemap_lock);
    free_map_release(indirect_block_pt->pointers[i],1);
    lock_release (&freemap_lock);
  }
  lock_acquire (&freemap_lock);
  free_map_release(inode_disk_pt->indirect,1);
  lock_release (&freemap_lock);

  free(indirect_block_pt);

  //Check if we are done
  if (sectors == 0){
    free(inode_disk_pt);
    return;
  }


  /* Deallocate db indirect sectors */
  //Get number of db indirect sectors needed to be deallocated
  size_t db_indirect_sectors = sectors;
  //Retrieve db indirect_block struct
  struct indirect_block * indirect_block_pt_2 = malloc(sizeof (struct indirect_block));
  cache_read(fs_device,inode_disk_pt->db_indirect,indirect_block_pt_2);

  //Iterate through each indirect pointer in db indirect_block struct
  for (size_t i = 0; i < DIV_ROUND_UP(db_indirect_sectors, POINTERS_IN_BLOCK); ++i)
  {
    //There are POINTERS_IN_BLOCK in one indirect block struct
    size_t sectors_2 =  POINTERS_IN_BLOCK;
    //Except the last iteration, we might have less than POINTERS_IN_BLOCK
    if (i == DIV_ROUND_UP(db_indirect_sectors, POINTERS_IN_BLOCK) - 1){
      sectors_2 = db_indirect_sectors;
    }

    db_indirect_sectors = db_indirect_sectors - sectors_2;
    //Retrieve indirect_block struct
    struct indirect_block * indirect_block_pt_3 = malloc(sizeof (struct indirect_block));
    cache_read(fs_device,indirect_block_pt_2->pointers[i],indirect_block_pt_3);
    //Deallocate the sectors one by one
    for (size_t j = 0; j < sectors_2; ++j)
    {
      lock_acquire (&freemap_lock);
      free_map_release(indirect_block_pt_3->pointers[j],1);
      lock_release (&freemap_lock);
    }
    
    lock_acquire (&freemap_lock);
    free_map_release(indirect_block_pt_2->pointers[i],1);
    lock_release (&freemap_lock);

    free(indirect_block_pt_3);

  }
  lock_acquire (&freemap_lock);
  free_map_release(inode_disk_pt->db_indirect,1);
  lock_release (&freemap_lock);

  free(indirect_block_pt_2);

  free(inode_disk_pt);
};


