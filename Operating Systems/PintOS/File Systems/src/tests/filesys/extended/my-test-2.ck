# -*- perl -*-
use strict;
use warnings;
use tests::tests;
use tests::random;
check_expected (IGNORE_EXIT_CODES => 1, [<<'EOF']);
(my-test-2) begin
(my-test-2) create "jerry"
(my-test-2) open "jerry"
(my-test-2) write "jerry"
(my-test-2) block_write without read !
(my-test-2) close "jerry"
(my-test-2) end
EOF
pass;
