/* -Proj3- Student testing. */
/* Test for optimization of block_write without reading the disk upon overwriting.
   Randomly generialize some bytes which is 100KB = 102400 Bytes (200 blocks). 
   Buffer cache should perform 200 calls to block_write, but 0 calls to block_read*/

#include <random.h>
#include <syscall.h>
#include "tests/lib.h"
#include "tests/main.h"

#define FILE_SIZE 120000
static char buf_jerry[FILE_SIZE];

static void
write_some_bytes (const char *file_name, int fd, const char *buf, size_t *ofs)
{
  if (*ofs < FILE_SIZE)
    {
      size_t block_size = random_ulong () % (FILE_SIZE / 8) + 1;
      size_t ret_val;
      if (block_size > FILE_SIZE - *ofs)
        block_size = FILE_SIZE - *ofs;

      ret_val = write (fd, buf + *ofs, block_size);
      if (ret_val != block_size)
        fail ("write %zu bytes at offset %zu in \"%s\" returned %zu",
              block_size, *ofs, file_name, ret_val);
      *ofs += block_size;
    }
}

void
test_main(void){
	int fd_jerry;
	size_t ofs_jerry = 0;

	random_init (0);
	random_bytes (buf_jerry, sizeof buf_jerry);

	CHECK (create ("jerry", 0), "create \"jerry\"");

	CHECK ((fd_jerry = open ("jerry")) > 1, "open \"jerry\"");

	msg ("write \"jerry\"");
	int old_read_count = readcount(0);
	int old_write_count = writecount(0);
  	while (ofs_jerry < FILE_SIZE)
  	  {
  	    write_some_bytes ("jerry", fd_jerry, buf_jerry, &ofs_jerry);
  	  }

  int total_read_count = readcount(0) - old_read_count;
	int total_write_count = writecount(0) - old_write_count;

	if (total_read_count == 0 && total_write_count > 200){
		msg("block_write without read !");
	}
    // msg("block read %d", total_read_count);
    // msg("block write %d", total_write_count);

  	msg ("close \"jerry\"");
  	close (fd_jerry);
 
}