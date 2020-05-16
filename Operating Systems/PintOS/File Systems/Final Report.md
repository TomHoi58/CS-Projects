Final Report for Project 3: File Systems
========================================

## Group Members

* Soung Bae Kim <sbkim@berkeley.edu>
* Houkit Hoi <tomhoi@berkeley.edu>
* Sanwu Luo <sanwuluo35@berkeley.edu>

## Task 1: Buffer Cache

### General Idea:
The cache is implemented on its own in `filesys/cache.c` and `filesys/cache.h`. It implements a full-associative cache of 64 entries with the clock replacement policy and the write-back policy. The biggest change from design documentation is that there is now both global and entry-specific locks being used to allow for simultaneous access to independent sectors.

`cache_read()`, `cache_write()`, `cache_read_at()`, and `cache_write_at()` are functions used by `inode.c` in order to access the blocks. Each of these functions will first look into the cache by calling `lookup_or_fetch_from_disk()` which accesses the cache and calls `block_read()` (`block_write()`) in the case of a miss. During a miss, `lookup_or_fetch_from_disk()` calls `find_usable_entry()` which writes back an entry in the cache if needed.

### Implementation Specifics:
In `cache.c`:
```c
  void cache_init (void);
  void cache_read (struct block *block, block_sector_t sector_id, void *buffer);
  void cache_write (struct block *block, block_sector_t sector_id, const void *buffer);
  void cache_read_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size);
  void cache_write_at (struct block *block, block_sector_t sector_id, void *buffer, off_t offset, off_t size);
  void cache_flush (struct block *block);

  struct cache_entry* lookup_or_fetch_from_disk(struct block *block, block_sector_t sector_id, bool read);
  struct cache_entry* look_up_sector_id(block_sector_t sector_id); // Called by lookup_or_fetch_from_disk
  struct cache_entry* find_usable_entry(struct block *block);
  void advance_clock_hand(void);

  struct cache_entry {
    block_sector_t sector_id;
    bool dirty;
    bool valid;              // Set to 0 (invalid) when the cache hasn't been touched at all.
    bool referenced;         // Indicate if it's been used recently. Used for clock replacement policy.
    struct lock entry_lock;
    char data[512];          // Stores the corresponding sector's data.
  };

  struct cache_entry cache_array[cache_max_size]; /* Representaion of the cache. */
  struct lock cache_lock; /* Global lock */
  int clock_hand; /* Used for clock replacement policy. */

```
**lookup_or_fetch_from_disk()**: This is call by all cache read/write functions in order to retrieve the corresponding cache entry. When called, it first acquires `cache_lock` then calls `look_up_sector_id()` to get the corresponding cache_entry. If it is a miss, `find_usable_entry()` is called which utilizes the clock replacement algorithm to find an invalid entry or empties one entry by writing back to disk. Then, the entry lock `entry_lock` is acquired. After acquiring the lock, we check once again with the `sector_id` to make sure it has not changed. `lookup_or_fetch_from_disk()` is called again if it has changed. Finally, it returns the `struct cache_entry *`.

**cache_read()**, **cache_write()**: First call `lookup_or_fetch_from_disk()`. On return, we are guaranteed to have the correct `cache_entry` with the correct `sector_id` with the `entry_lock` acquired. Therefore, we simply read/write the the returned `cache_entry`.

**cache_read_at()** and **cache_write_at()** are just like their counter parts but with sizes smaller than BLOCK_SECTOR_SIZE.


## Task 2: Indexed and Extensible Files
We basically follow our design doc for task two. However, we implement two helper functions to simplify `inode_resize()` (which is `inode_allcoate()` in design doc). `allocate_indirect()` is a helper funciton that allocate indirect blocks. By using a for loop, we allocate every direct block pointer inside of indirect block by using `free_map_allocate()` with amount of 1.  `allocate_db_indirect()` is a helper function that use `allocate_indirect()` in the for loop to allocate indirect blocks along with the direct blocks inside of them. `inode_resize()` is our main funciton to allocate and extend the `struct inode_disk`. Inside of `inode_resize()`, we keep track of `new_sectors` to help us determine which part of the blocks we should allocate. Each time we allocate a new block, we decrement the `new_sectors` to make sure we know how many blocks are needed. Firstly, we allocate the direct blocks using a for loop. Then we compare `new_sectors` with bound limite and initialize `indirect_block` and `doubly_indirect_block` if they are needed. After we have initialized `indirect_block` and `doubly_indirect_block`, we use `allocate_indirect()` and `allocate_db_indirect()` to allocate indirect blocks and doubly indirect block.

  We change `byte_to_sectors()` to calculate block device sector that contains byte offset POS within the inode. We count the total index of sector by using `pos \ BLOCK_SECTOR_SIZE` and store it as `sector_th`. Then, we walk through our block device sector in the order of direct blocks, indirect blocks, and doubly indirect blocks. Then we return the correct block device if we are in the right block sector.

  We also change `inode_create` to replace the `free_map_allocate()` with `inode_resize()` to accomodate uncontinuous block sector storage.

  `inode_write_at()` is changed by adding a check on whether we need to increment the file size. We get our inode_disk structure by `get_inode_disk()` and use `byte_to_sectors()` to see if we need more blocks. After we have extended the file size, we write at the sector that's needed.

  We create a helper funciton `inode_deallocate()` in `inode_close()`. The algorithm of `inode_deallocate()` is basically the same as `inode_resize()`. We keep track of how many sectors are needed to free by creating the variable `sectors`. Then we traverse through the direct block sectors, indirect block sectors, and doubly indirect block sectors to free each block though calling `free_map_release()` with amount of 1.

  For synchronization, we use freemap_lock to make sure that each inode operation is exclusively when it's accessing free_map. For example, every time we want to allocate a sector using `free_map_allocate()`, we acquire the freemap_lock first, and release it after we are done.

## Task 3: Subdirectories

### General Idea:
Directories are implemented over inodes much like how files are. Each directory will be unique in that the data being kept are solely `struct dir_entry`s. The first entry will always be itself '.' and the second entry will always be its parent '..'.

We allow implement a thread's current working directory by storing a `struct dir *cwd` within its `struct thread`. The `cwd` of the initial thread is automatically set to the root directory and on `exec()`, the parent thread's `cwd` is copied over.

For each system call, we first validate the arguments as in Project 1. We then start at either the root or `cwd` depending on the path given. With the correct directory, the corresponding function in `filesys.h`, `file.h`, or `directory,h` was called.

### Implementation Specifics:
In `directory.c`, we add the following functions:
```c
  int get_next_part (char part[NAME_MAX + 1], const char **srcp);
  void support_myself_and_parent(struct dir *parent, struct dir *myself);
  struct dir *dir_getter(char *path, char filename[NAME_MAX + 1]);
```
`dir_getter()` traverses the filesystem with the helper of the given `get_next_part()` to reach the correct directory. This is done by calling `dir_open()`, `dir_lookup()`, and `dir_close()` repeatedly until the last directory is reached. We are then able to complete the syscall by calling the corresponding function in any of the three filesystem classes mentioned above. Note that each directory will have two initial `dir_entry`s through `support_myself_and_parent()`.

Changes were also made in `filesys.c` in order to support relative pathing and subdirectories. `filesys_open()`, `filesys_create()`, and `filesys_remove()` now expects an additionally parameter `struct dir *dir` which replaces the `dir` begin initialized to the root directory.


In `syscall.c`, we add the following cases for the addition syscalls:
```c
  SYS_CHDIR,                  /* Change the current directory. */
  SYS_MKDIR,                  /* Create a directory. */
  SYS_READDIR,                /* Reads a directory entry. */
  SYS_ISDIR,                  /* Tests if a fd represents a directory. */
  SYS_INUMBER,                /* Returns the inode number for a fd. */
```
**SYS_CHDIR**: Find the corresponding directory and set it to `thread_current()->cwd`. Close the old `cwd` <br>
**SYS_MKDIR** : Find the corresponding directory which in this case is the second to last part in the path. Calling `filesys_create()` creates an inode which set to be a directory later on by setting the boolean `dir` in `struct disk_inode`. Then, `support_myself_and_parent()` and `dir_add()` are called to keep the filesystem consistent<br>
**SYS_READDIR**: Find the corresponding directory and call `dir_readdir()`.<br>
**SYS_ISDIR**: Simply check the `dir` boolean on the corresponding inode's `struct inode_disk`.<br>
**SYS_INUMBER**: Get the inode of the file or directory, then output the sector number of the inode through the `struct inode`.<br>

## Student Testing

### my-test-1

**Desciption :** Test the buffer cache's effectiveness by measuring two cache hit rates from reading the same file twice.<br>
**Overview :** Generalize some random bytes into a buffer which would be written into a newly created file, which is no larger than our cache size. Then open the file and read it with cold cache, record the hit rate. Reopen the file and read it again without resetting the cache, record the hit rate and compare with the first hit rate. The test expect it to increase.<br>
**Output :**
```
Copying tests/filesys/extended/my-test-1 to scratch partition...
Copying tests/filesys/extended/tar to scratch partition...
qemu-system-i386 -device isa-debug-exit -hda /tmp/2RXv9moSYt.dsk -hdb tmp.dsk -m 4 -net none -nographic -monitor null
PiLo hda1
Loading...........
Kernel command line: -q -f extract run my-test-1
Pintos booting with 3,968 kB RAM...
367 pages available in kernel pool.
367 pages available in user pool.
Calibrating timer...  418,611,200 loops/s.
hda: 1,008 sectors (504 kB), model "QM00001", serial "QEMU HARDDISK"
hda1: 191 sectors (95 kB), Pintos OS kernel (20)
hda2: 244 sectors (122 kB), Pintos scratch (22)
hdb: 5,040 sectors (2 MB), model "QM00002", serial "QEMU HARDDISK"
hdb1: 4,096 sectors (2 MB), Pintos file system (21)
filesys: using hdb1
scratch: using hda2
Formatting file system...done.
Boot complete.
Extracting ustar archive from scratch device into file system...
Putting 'my-test-1' into the file system...
Putting 'tar' into the file system...
Erasing ustar archive...
Executing 'my-test-1':
(my-test-1) begin
(my-test-1) create "tom"
(my-test-1) open "tom"
(my-test-1) write "tom"
(my-test-1) close "tom"
(my-test-1) reset cache
(my-test-1) open "tom"
(my-test-1) read "tom" with cold cache
(my-test-1) close "tom"
(my-test-1) open "tom"
(my-test-1) read "tom" with hot cache
(my-test-1) close "tom"
(my-test-1) The hit rate improves !
(my-test-1) end
my-test-1: exit(0)
Execution of 'my-test-1' complete.
Timer: 75 ticks
Thread: 16 idle ticks, 57 kernel ticks, 2 user ticks
hdb1 (filesys): 75 reads, 526 writes
hda2 (scratch): 243 reads, 2 writes
Console: 1321 characters output
Keyboard: 0 keys pressed
Exception: 0 page faults
Powering off...
```
**Result :**
```
PASS
```
**Potential kernel bugs :**
1. If we forget to set the valid bit to true after using it, the kernel wouldn't use any of cached data since it alwasys thinks that they invalid. Then it has to get the data from the disk everytime. In this case, the hit rate wouldn't increase, so the test would fail and output `The hit rate doesn't improve !` instead. 

2. If we somehow don't rotate the clock hand correctly, for example, if we use `mod 6` instead of `mod 64` after incrementing the clock hand. The kernel would only use the first 6 cache entries to store data, which is too small to fit the entire file. In this case, the cache wouldn't help while we read the file sequentially in the second time since the cache entries only have the last part of the file while we are reading the first part of the file. 

### my-test-2
**Desciption :** Test the buffer cache to see if it write full blocks to disk without reading them first by measuring the number of calls to `block_read` and `block_write`.<br>
**Overview :** Generalize some random bytes into a buffer which would be written into a newly created file, which is 120000 bytes (larger than 200 blocks). Before writing to the file, we measure the old values of read count and write count. After writing, we measure them and subtract the values the get the the number of calls to `block_read` and `block_write`. The key of the test is to see if the `block_read` count is 0, so if we have `block_read` count is 0 and `block_read` count > 200, the test will pass.<br>
**Output :**
```
Copying tests/filesys/extended/my-test-2 to scratch partition...
Copying tests/filesys/extended/tar to scratch partition...
qemu-system-i386 -device isa-debug-exit -hda /tmp/xzTzL0JKMj.dsk -hdb tmp.dsk -m 4 -net none -nographic -monitor null
PiLo hda1
Loading...........
Kernel command line: -q -f extract run my-test-2
Pintos booting with 3,968 kB RAM...
367 pages available in kernel pool.
367 pages available in user pool.
Calibrating timer...  447,283,200 loops/s.
hda: 1,008 sectors (504 kB), model "QM00001", serial "QEMU HARDDISK"
hda1: 191 sectors (95 kB), Pintos OS kernel (20)
hda2: 242 sectors (121 kB), Pintos scratch (22)
hdb: 5,040 sectors (2 MB), model "QM00002", serial "QEMU HARDDISK"
hdb1: 4,096 sectors (2 MB), Pintos file system (21)
filesys: using hdb1
scratch: using hda2
Formatting file system...done.
Boot complete.
Extracting ustar archive from scratch device into file system...
Putting 'my-test-2' into the file system...
Putting 'tar' into the file system...
Erasing ustar archive...
Executing 'my-test-2':
(my-test-2) begin
(my-test-2) create "jerry"
(my-test-2) open "jerry"
(my-test-2) write "jerry"
(my-test-2) block_write without read !
(my-test-2) close "jerry"
(my-test-2) end
my-test-2: exit(0)
Execution of 'my-test-2' complete.
Timer: 90 ticks
Thread: 18 idle ticks, 65 kernel ticks, 7 user ticks
hdb1 (filesys): 40 reads, 726 writes
hda2 (scratch): 241 reads, 2 writes
Console: 1137 characters output
Keyboard: 0 keys pressed
Exception: 0 page faults
Powering off...
```
**Result :**
```
PASS
```

**Potential kernel bugs :**
1. If the cache has exact same implementation of fetching block data from disk for `cache_read` and `cache_write`, the kernel would read in data from the disk even though we are overwriting the data. In this case, the numnber of calls to `block_read` would be around the same as numnber of calls to `block_write`. The test would fail and the line of `block_write without read !` would be missing. 

2. If we forget to release the global cache lock somewhere after acquiring it, the kernel would get stuck, thus, wouldn't output anything. The test will fail as well.

## Reflection:
Overall, the project took much longer than we had anticipated which is why we were not able to debug Task 3 completely. Although we each tried to implement each parts of the project together, it was difficult to share the working progress with each other due to the current situation. This in turn made debugging and sharing ideas together much more difficult. As a final note, although Soung Bae and Sam (Sanwu) tried their best, Tom (Houkit) was the superstar for this project.
