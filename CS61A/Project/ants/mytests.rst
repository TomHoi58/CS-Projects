This file holds the tests that you create. Remember to import the python file(s)
you wish to test, along with any other modules you may need.
Run your tests with "python3 ok -t --suite SUITE_NAME --case CASE_NAME -v"
--------------------------------------------------------------------------------

Suite 1



	Case Example
		>>> from ants import *
        >>> hive, layout = Hive(AssaultPlan()), dry_layout
        >>> dimensions = (1, 9)
        >>> colony = AntColony(None, hive, ant_types(), layout, dimensions)
        >>> # Testing Scare
        >>> error_msg = "ScaryThrower doesn't scare for exactly two turns."
        >>> scary = ScaryThrower()
        >>> bee = Bee(3)
        >>> colony.places["tunnel_0_0"].add_insect(scary)
        >>> colony.places["tunnel_0_4"].add_insect(bee)
        >>> scary.action(colony)
        >>> bee.action(colony)
        False


