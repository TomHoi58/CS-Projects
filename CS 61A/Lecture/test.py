def split(n):
    return n//10, n%10

def sum_digit(n):
    if n <10:
        return n
    else:
        all_but_last, last = split(n)
        return sum_digit(all_but_last)+last




def fac(n):
    if n == 0 :
        return 1
    else:
        return n*fac(n-1)



def lum_sum(n):
    if n <10:
        return n
    else:
        all_but_last, last = split(n)
        return last + lum_sum_double(all_but_last)

def lum_sum_double(n):
    if n < 10:
        return 2*n
    else:
        all_but_last, last = split(n)
        return sum_digit(2*last) + lum_sum(all_but_last)


def fib(n):
    if n < 2:
        return n
    else:
        return fib(n-1)+fib(n-2)

def fib_dot(n):
    if n < 2:
        return n
    else:
        print(str(n)+ '->'+str(n-2))
        print(str(n) + '->' + str(n - 1))
        return fib_dot(n-1)+fib_dot(n-2)

def fib_tree(n):
    print('diagram{')
    fib_dot(n)
    print('}')

def pascal(row, column):
    """Returns a number corresponding to the value at that location
    in Pascal's Triangle.
    >>> pascal(0, 0)
    1
    >>> pascal(0, 5)	# Empty entry; outside of Pascal's Triangle
    0
    >>> pascal(3, 2)	# Row 4 (1 3 3 1), 3rd entry
    3
    """

    "*** YOUR CODE HERE ***"
    if column <0 or row < column :
        return 0
    if column == 0 and row ==0:
        return 1
    else:
        return pascal(row-1,column-1)+pascal(row-1,column)


def count_while(s,value):
    total, index = 0,0
    while index < len(s):
        if s[index]== value:
            total += 1
        index += 1
    return total



def count_while(s,value):
    total =0
    for element in s:
        if element == value:
            total +=1
    return total


def sum_below(n):
    total =0
    for i in range(n):
        total += i
    return total

def cheer():
    for _ in range(3):
        print("i can't stop repeating myself")

def divisors(n):
    return [x for x in range(1,n+1) if n%x== 0]




def count_partitions(total, biggest_num):
    if total == 0:
        return 1
    elif total < 0:
        return 0
    elif biggest_num == 0:
        return 0
    else:
        with_biggest = count_partitions(total - biggest_num, biggest_num)
        without_biggest = count_partitions(total, biggest_num - 1)
        return with_biggest + without_biggest

def partitions_options(total, biggest_num):
    if total == 0:
        return [[]]
    elif total < 0:
        return []
    elif biggest_num == 0:
        return []
    else:
        with_biggest = partitions_options(total - biggest_num, biggest_num)
        without_biggest = partitions_options(total, biggest_num - 1)
        with_biggest = [[biggest_num] + elem for elem in with_biggest] # what extra step do we need here?
        return with_biggest + without_biggest



def accumulate(lst):
    cum_sum=0
    for i in lst:
        if isinstance(i,list):
            cum_sum += accumulate(i)
        else:
            cum_sum += i
    return cum_sum


def make_counter(name):
    x = 0
    def counter():
        nonlocal x
        x +=1
        return name, x
    return counter

def memo(f):
    cache = {}

    def memoized(n):
        if n not in cache:
            cache[n] = f(n)
        return cache[n]

    return memoized

def fib(n):
    if n == 0 or n == 1:
        return n
    else:
        left = fib(n - 2)
        right = fib(n - 1)
        return left + right

fib = memo(fib)


class Bird:
    def __init__(self, call):
        self.call = call
        self.can_fly = True
    def fly(self):
        if self.can_fly:
            return "Don't stop me now!"
        else:
            return "Ground control to Major Tom..."

    def speak(self):
        print(self.call)

class Chicken(Bird):
    def speak(self, other):
        Bird.speak(self)
        other.speak()

class Penguin(Bird):
    can_fly = False
    def speak(self):
        call = "Ice to meet you"
        print(call)

def splice(a,b,k):
    return a[:k]+b+a[k:]

def all_splice(a,b,c):
    return [i for i in range(len(a)) if splice(a,b,i)==c]


earth = [0]
earth.append([earth])

def wind(fire, groove):
    fire[1][0][0] = groove
    def fire():
        nonlocal fire
        fire = lambda fantasy: earth.pop(1).extend(fantasy)
        return fire(groove)
    return fire()
sep = earth[1]
wind(earth, [earth[0]] + [earth.append(0)])

def can_win(number):
    if number <= 0:
        return False
    action = 1
    while action <= 3:
        new_state = number - action
        if not can_win ( new_state ):
            return True
        action += 1
    return False

print(can_win(6))



def can_win2(number):
    if number <=0:
        return False
    elif 1<= number <= 3:
        return True
    else:
        return not any([can_win2(number-1),can_win2(number-2),can_win2(number-3)])

print(can_win2(6))


class Pair:
    def __init__(self, first, second):
        self.first = first
        if not scheme_valid_cdrp(second):
            raise SchemeError("cdr can only be a pair, nil, or a promise but was {}".format(second))
        self.second = second

    def map(self, fn):
        assert isinstance(self.second, Pair) or self.second is nil, \
            "Second element in pair must be another pair or nil"
        return Pair(fn(self.first), self.second.map(fn))

    def __repr__(self):
        return 'Pair({}, {})'.format(self.first, self.second)

class nil:
    def map(self, fn):
        return nil

    def __getitem__(self, i):
        raise IndexError('Index out of range')

    def __repr__(self):
        return 'nil'

nil = nil()



def game(cards):
    def helper(cards,p1,p2):
        if not cards:
            return p1==p2
        return any([helper(cards[1:],p2,p1+cards[0]),helper(cards[:-1],p2,p1+cards[-1])])
    return helper(cards,0,0)

def all_ways_gem(lst,n):
    if n == 0:
        yield []
    elif not lst:
        return
    else:
        first_el = lst[0]
        yield from all_ways_gem(lst[1:],n)
        for s in all_ways_gem(lst[2:],n-first_el):
            yield [first_el]+s

g= all_ways_gem([1, 6, 4, 7, 2, 3], 7)
print(list(g))

class Tree:
    def __init__(self, label, branches=[]):
        self.label = label
        for branch in branches:
            assert isinstance(branch, Tree)
        self.branches = list(branches)

    def is_leaf(self):
        return not self.branches

def tree_greater_than(t1,t2):
    if t1.label > t2.label:
        return 1 + sum([tree_greater_than(t1.branches[i],t2.branches[i]) for i in range(len(t1.branches))])
    return sum([tree_greater_than(t1.branches[i],t2.branches[i]) for i in range(len(t1.branches))])

class Link:
    empty = ()

    def __init__(self, first, rest=empty):
        self.first = first
        self.rest = rest

    def __len__(self):
        return 1 + len(self.rest)

    def __repr__(self):
        return "Link({}, {})".format(self.first, self.rest)

def double_double(lst):
    if lst is not Link.empty:
        lst.first = 2* lst.first
        lst.rest = Link(lst.first, lst.rest)
        double_double(lst.rest.rest)

a = Link(1, Link(2, Link(3)))
double_double(a)
print(a)
