# -*- perl -*-
use strict;
use warnings;
use tests::tests;
use tests::random;
check_expected (IGNORE_EXIT_CODES => 1, [<<'EOF']);
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
EOF
pass;
