Design Document for Project 3: File Systems
===========================================

## Group Members

* Soung Bae Kim <sbkim@berkeley.edu>
* Houkit Hoi <tomhoi@berkeley.edu>
* Sanwu Luo <sanwuluo35@berkeley.edu>

## Task 1: Buffer Cache
### Data Structures & Functions
We define `struct cache_entry` and create a new file **cache.c**:

```c
#define cache_max_size 64 //sectors 
struct hash hashmap;
unsigned int clock_hand;
struct lock entire_cache_lock;
struct semaphore sema; 
```

```c
struct cache_entry {
	block_sector_t sector_id;
	bool referenced;
	bool dirty;
	bool removed;
	struct lock cache_entry_lock;
	struct hash_elem elem;
	void data[512];
};

```

Functions defined in **cache.c**;
```c
void cache_init() {
	//Initialize hashmap with our own defined functions
	//Initialize clock hand
	//Initialize entire_cache_lock
	//Initialize sema to cache_max_size
}

void cache_read(struct block *block, block_sector_t sector, void *buffer, off_t offset, size_t size) {
	//Check if the requested sector is in the cache, if so, read it
	//Else, read the whole sector from the block device to some cache block 
	//Load the data to buffer based on its requested offset and size
}

void cache_write(struct block *block, block_sector_t sector, void *buffer, off_t offset, size_t size) {
	//Check if the requested sector is in the cache, if so, write it from buffer
	//Else, read the whole sector from the block device to some cache block 
	//Write the data to the cache from buffer based on its requested offset and size
}

void cache_flush() {
	//Iterate through the hashmap and write all the data back to the block device if it's dirty by calling block_write
}

/* Some other helper functions. */
```

Functions modified in **inode.c**:
```c
void
inode_init (void)
{
  ...
  cache_init();
  ...
}

static block_sector_t byte_to_sector (const struct inode *inode, off_t pos);
struct inode * inode_open (block_sector_t sector);
bool inode_create (block_sector_t sector, off_t length);
off_t inode_read_at (struct inode *inode, void *buffer_, off_t size, off_t offset);
off_t inode_write_at (struct inode *inode, const void *buffer_, off_t size, off_t offset);
```

Functions modified in **filesys.c**;
```c
void
filesys_done (void)
{
  ...
  cache_flush();
  ...
}
```

### Algorithms
All the functions in **inode.c** that have `block_write` and `block_read` should be changed to `cache_write` and `cache_read` based on their respective logic. 

`cache_read`: First check if the sector is in the cache by using `hash_find`. If so, save the element and set referered = true. Then read the data stored in `cache_entry.data` into the buffer. If the cache doesn't have the sector and the hash size  < 64, create a `struct cache_entry` with `block_read`, then insert it to the hashmap. If hashmap.size = 64, run clock algorithm by iterating through the hash starting from the `clock_hand`. If the referenced = true, set it to false then run to next one. Else, replace the `cache_entry`with `referered` = true. If the evicted entyr is dirty, call `block_write`.

`cache_write`: First check if the sector is in the cache like in `cache_read`. If so, modify the corresponding `cache_entry.data` using `block_write` and set `dirty = true`, `referered = true`. If the cache doesn't have the sector, pull if from the block to the cache and follow similar logic to `cache_read` if the cache gets full. Finally, write it to the cache from buffer.

`byte_to_sector`: Since storing `struct inode_disk data` in `struct inode` is against the 64-block limit. We removed it from the struct. Instead, we use `inode_read_at` to retrieve the `inode_disk` and access its `start`.

### Synchronization
Whenever we are modifying or iterating through the hashmap, we need to acquire `entire_cache_lock` to ensure no more than one process is evicting and loading to the cache. While after we find/load/evict the element from the cache hashmap, we release `entire_cache_lock` and acquire `cache_entry_lock` and the independent operations can run concurrently without waiting. In order to prevent the cache being evicted while its being, we acquire `cache_entry_lock` whenever we are trying to access or modify the entry's data like `bool referenced, bool dirty, void data[512]`. In this way, the other threads have to wait if they are trying to access the same entry. If they are executing the clock algorithm to evict the entry, they have to go to next one if the entry is locked. We also make sure it calls `sema_down` if the entry is being accesed, so that new thread has to wait by calling `sema_down` before running the clock algorithm when all cache entries are being accessed. `sema_up` after each access to entries.

### Rationale
We thought of using a list to save the `struct cache_entry` but it would be troublesome to iterate through the list to find a certain element. Therefore, we decided to use hashmap so that getting a certain element based on the key is more convenient and it's also more like a associative cache (although in real life it uses hard ware comparator). Clock algorithm is easier to implement using the list index and mod arithmetic compared to others like second-chance list. Other than putting all new data structures in `inode.c`, we creating `cache.c` and `cache.h`. In this way, it would be very easy to conceptualize and easy to accommodate if we need to use it elsewhere.

### Questions for TA
1. Does the maximum capacity of 64 disk blocks count for some metadata that we want to store for cache blocks?

2. Is using a semaphore to make the 65th process wait if all cache entried are being accessed a good idea? 

## Task 2: Indexed and Extensible Files
### Data Structures & Functions
```C
struct inode_disk {
  ...
  block_sector_t direct[124];           /* Direct pointers. */
  block_sector_t indirect;              /* Indirect pointer. */
  block_sector_t doubly_indirect;       /* Doubly indirect pointer. */
  ...
};
struct indirect_block {
  block_sector_t blocks[128];
};



bool inode_allocate(struct inode_disk *inode_disk, off_t length)
bool inode_deallocate(struct inode *inode)
```

### Algorithms
#### `inode_allocate()`
`inode_allocate()` is called in two circumstances to support extension of the file. First is inside of `inode_create`, replacing `free_map_allocate`. The second one is inside of `inode_write_at` when past the EOF so that we can extend the file.
When we are allocating the blocks, we will allocate in the order of direct->indirect->doubly-indirect based on the needed size of the sapce.
In the first scenario, creating a file, we first calculate the number of sectors needed by calling `byte_to_sectors`. Then starting with the `direct_block`, we allocate the blocks in order by calling `free_map_allocate`. When all the `direct_block` are used up, we continue to use `struct indirect_block`. When all `indirect_block` is depleted, `doubly_indirect block` will be used.
In the second scenario, extending a file, we go through each sector and allocate if it hasn't been allocated before.

#### `inode_deallocate()`
We first calculate the the number of sectors occupied by inode through calling `bytes_too_sector`. Following the order of direct->indirect->doubly-indirect blocks, we use `free_map_release` to free all the used block.

Due to our new implementation of `inode_deallocate` and `inode_allocate`, we need to make change to `inode_create` and `inode_close` to replace `free_map_allocate` and `free_map_release` with `inode_allocate` and `inode_deallocate`.

#### Changes to `inode_write_at()`
To support file extension, we first check whether `byte_to_sector((` return -1 or not. If that's the case, it means that we are writing past EOF. We call `inode_allocate` to allocate addition blocks that are needed.
### Synchronization
For synchronization, every time when gonna use `free_map_flush()`, `free_map_allocate()`, and `free_map_release()`, we acquire responding lock in `cache_entry` to prevent any conflict. We can make a copy of freemap inside of cache to make this happen.


### Rationale
The direct pointers in `inode_disk` enables to us have faster access for shorter files. The `struct indirect_block` will hold 128 `block_sector_t` and doubly-indirect block will hold 128 indirect_blocks so that we will have enough space for 8 MB. File extension can be easily achieved because `inode_disk` holds the sector numbers.

## Task 3: Subdirectories
### Data Structures & Functions
In **thread.h**:
  ```c
  struct thread {
    ...
    struct dir *cwd;
    ...
  };
  ```
In **inode.c**, we add:
 ```c
 struct inode_disk {
    ...
    bool is_dir;
    ...
 };
 ```

In **syscall.c**: `chdir()`, `mkdir()`, `readdir()`, `isdir()`, `open()` (modify), `close()` (modify)

In addition, we add the function `get_next_part()` as implemented in the spec to pull out each part of the filename.

### Algorithms
Since our filesystem now has subdirectories, each thread will keep track of its current working directory (`struct dir *cwd`). Whenever the relative path is not passed in, we will use the `get_next_part()` function to parse through the filename and arrive at the corresponding inode. For example, if we get a request for the directory `../src/`, we will first load in the inode corresponding to the parent directory (2nd block). Then, we will find the block corresponding to `src`. Now that each thread has a current working directory, any filesystem calls will have to act relative to that directory.
  
For subdirectories:

Each subdirectory can simply be seen as another inode with the same extension mechanism as the files in Task 2 with the only difference being the `is_dir` member. Thus, each subdirectory will be represented by an inode where each `block_sector_t` listed will be one of the directory's entries. Therefore to access a subdirectory, we must travel down from the root directory by looking at each inodes and traversing down to the `block_sector_t` that corresponds to the directory's path. 

For the syscalls:
  
`chdir()`: Change `cwd` into the corresponding directory by calling `dir_close()` on the current directory, and calling `dir_open()` on the new current directory.

`mkdir()`: Create a new entry in the inode corresponding to the `cwd` in current thread by calling `dir_add()` on the current directory. Whenever a new directory is made, we will create a new inode of size 2 which will contain pointers to itself (.) and the parent directory (..). These pointers will be `block_sector_t` of itself and its parent, respectively. The size of the directory will grow as files and subdirectories are added.

`readdir()`: Step through each of the inodes of the tree and return the list of all the entries

`isdir()`:  Check to see if the corresponding inodes is a directory by seeing the `is_dir` member in the inode.

`open()`: Modify such that if the given name is a directory, calls `dir_open()`.

`close()`: Modify such that it calls `dir_close()`.

`remove()`: When we remove a directory, the `remove` bit is set by the cache. The cache will prevent other processes from making changes to that `block_sector_t`.

### Synchronization
Just as in Task 2, the main synchronization will be taken care by the cache since every request for a `block_sector_t` will be processed by the cache anyways. Thus, whenever there is a change in `block_sector_t` that represents the directory, the cache will lock the resource correctly. 
 
### Rationale
  This allows be in line with the inodes we implemented for task 2 since subdirectories are just a special type of inodes. Consequently, this allows for subdirectories of arbitrary sizes.

## Additional Question:
### Write-behind
Our project 2 timer implementation basically has what we need for writing back to disk periodically. Let say if we want to flush the cache every 20 ticks, the implementation would be: On the line right after `ticks++` in function `timer_interrupt`, we check if ticks mod 20 is 0, if so we call `cache_flush()` to save the data back to the disk.

### Read-ahead
To order to read the future data the process might need asynchronously, we create a thread in `inode_read_at` and let it run our new function `read_head`, which has the similar functionalities as `inode_read_at` does. The arguments that we pass in would be inherited from its original thread except the offset being 512, since 512 would be the next block that the process might potentially read in the future. 
