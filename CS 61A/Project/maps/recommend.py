"""A Yelp-powered Restaurant Recommendation Program"""

from abstractions import *
from data import ALL_RESTAURANTS, CATEGORIES, USER_FILES, load_user_file
from ucb import main, trace, interact
from utils import distance, mean, zip, enumerate, sample
from visualize import draw_map

##################################
# Phase 2: Unsupervised Learning #
##################################


def find_closest(location, centroids):
    """Return the centroid in centroids that is closest to location.
    If multiple centroids are equally close, return the first one.

    >>> find_closest([3.0, 4.0], [[0.0, 0.0], [2.0, 3.0], [4.0, 3.0], [5.0, 5.0]])
    [2.0, 3.0]
    """
    # BEGIN Question 3
    "*** YOUR CODE HERE ***"
    #orginal codes
    # "creat a list for distances from location to each centroids"
    # distance_list = [distance(location,i) for i in centroids]
    # "bind each centroids to its corresponding distance in a list"
    # distance_list_with_centroids = zip(distance_list, centroids)
    # "use the min function to figure out the closest centroids"
    # return min(distance_list_with_centroids,key= lambda x: x[0])[1]

    #revised codes
    from math import sqrt
    return min(centroids,key= lambda x: sqrt((x[0]-location[0])**2+(x[1]-location[1])**2))

    # END Question 3


def group_by_first(pairs):
    """Return a list of lists that relates each unique key in the [key, value]
    pairs to a list of all values that appear paired with that key.

    Arguments:
    pairs -- a sequence of pairs

    >>> example = [ [1, 2], [3, 2], [2, 4], [1, 3], [3, 1], [1, 2] ]
    >>> group_by_first(example)  # Values from pairs that start with 1, 3, and 2 respectively
    [[2, 3, 2], [2, 1], [4]]
    """
    keys = []
    for key, _ in pairs:
        if key not in keys:
            keys.append(key)
    return [[y for x, y in pairs if x == key] for key in keys]


def group_by_centroid(restaurants, centroids):
    """Return a list of clusters, where each cluster contains all restaurants
    nearest to a corresponding centroid in centroids. Each item in
    restaurants should appear once in the result, along with the other
    restaurants closest to the same centroid.
    """
    # BEGIN Question 4
    "*** YOUR CODE HERE ***"
    "find out the closest centroids for each restaurant"
    centroids_list= [find_closest(restaurant_location(r),centroids) for r in restaurants]
    "bind them to their corresponding restaurants"
    centroids_list_with_restaurants= zip(centroids_list,restaurants)
    "group them by different centroids"
    return group_by_first(centroids_list_with_restaurants)



    # END Question 4


def find_centroid(cluster):
    """Return the centroid of the locations of the restaurants in cluster."""
    # BEGIN Question 5
    "*** YOUR CODE HERE ***"
    "make a list of latitude of all restaurants"
    latitude_list=[restaurant_location(i)[0] for i in cluster]
    "make a list of longitude of all restaurants"
    longitude_list=[restaurant_location(i)[1] for i in cluster]
    "calculate the average lat and long and return the new centroid"
    return [mean(latitude_list),mean(longitude_list)]

    # END Question 5


def k_means(restaurants, k, max_updates=100):
    """Use k-means to group restaurants by location into k clusters."""
    assert len(restaurants) >= k, 'Not enough restaurants to cluster'
    old_centroids, n = [], 0

    # Select initial centroids randomly by choosing k different restaurants
    centroids = [restaurant_location(r) for r in sample(restaurants, k)]

    while old_centroids != centroids and n < max_updates:
        old_centroids = centroids
        # BEGIN Question 6
        "*** YOUR CODE HERE ***"
        "generate a list of lists of restaurants by centroids"
        restaurants_in_groups = group_by_centroid(restaurants,centroids)
        "generate new centroids"
        centroids = [find_centroid(i) for i in restaurants_in_groups]

        # END Question 6
        n += 1
    return centroids


################################
# Phase 3: Supervised Learning #
################################


def find_predictor(user, restaurants, feature_fn):
    """Return a rating predictor (a function from restaurants to ratings),
    for a user by performing least-squares linear regression using feature_fn
    on the items in restaurants. Also, return the R^2 value of this model.

    Arguments:
    user -- A user
    restaurants -- A sequence of restaurants
    feature_fn -- A function that takes a restaurant and returns a number
    """
    xs = [feature_fn(r) for r in restaurants]
    ys = [user_rating(user, restaurant_name(r)) for r in restaurants]

    # BEGIN Question 7
    "*** YOUR CODE HERE ***"
    "Compute Sxx, Syy, Sxy"
    Sxx = sum([(feature_fn(i) - mean(xs)) ** 2 for i in restaurants])
    Syy = sum([(user_rating(user, restaurant_name(i)) - mean(ys)) ** 2 for i in restaurants])
    Sxy = sum([((feature_fn(i) - mean(xs))*(user_rating(user, restaurant_name(i)))) for i in restaurants])
    "Compute b, a, r_squared"
    b = Sxy/Sxx
    a = mean(ys)-b*mean(xs)
    r_squared = Sxy**2 / (Sxx*Syy)


    # END Question 7

    def predictor(restaurant):
        return b * feature_fn(restaurant) + a

    return predictor, r_squared


def best_predictor(user, restaurants, feature_fns):
    """Find the feature within feature_fns that gives the highest R^2 value
    for predicting ratings by the user; return a predictor using that feature.

    Arguments:
    user -- A user
    restaurants -- A list of restaurants
    feature_fns -- A sequence of functions that each takes a restaurant
    """
    reviewed = user_reviewed_restaurants(user, restaurants)
    # BEGIN Question 8
    "*** YOUR CODE HERE ***"
    "make a list of feature_fns with their R^2"
    predictors_list = [find_predictor(user,reviewed,i) for i in feature_fns]
    "choose the feature_fn with the higest R^2"
    return max(predictors_list,key=lambda x: x[1])[0]
    # END Question 8


def rate_all(user, restaurants, feature_fns):
    """Return the predicted ratings of restaurants by user using the best
    predictor based on a function from feature_fns.

    Arguments:
    user -- A user
    restaurants -- A list of restaurants
    feature_fns -- A sequence of feature functions
    """
    predictor = best_predictor(user, ALL_RESTAURANTS, feature_fns)
    reviewed = user_reviewed_restaurants(user, restaurants)
    # BEGIN Question 9
    "*** YOUR CODE HERE ***"
    rating_list=[]
    for r in restaurants:
        name = restaurant_name(r)
        if r in reviewed:
            rating_list.append(user_rating(user,name))
        else:
            rating_list.append(predictor(r))
    return dict(zip([restaurant_name(r) for r in restaurants],rating_list))
    # END Question 9


def search(query, restaurants):
    """Return each restaurant in restaurants that has query as a category.

    Arguments:
    query -- A string
    restaurants -- A sequence of restaurants
    """
    # BEGIN Question 10
    "*** YOUR CODE HERE ***"
    return [i for i in restaurants if query in restaurant_categories(i)]
    # END Question 10


def feature_set():
    """Return a sequence of feature functions."""
    return [lambda r: mean(restaurant_ratings(r)),
            restaurant_price,
            lambda r: len(restaurant_ratings(r)),
            lambda r: restaurant_location(r)[0],
            lambda r: restaurant_location(r)[1]]


@main
def main(*args):
    import argparse
    parser = argparse.ArgumentParser(
        description='Run Recommendations',
        formatter_class=argparse.RawTextHelpFormatter
    )
    parser.add_argument('-u', '--user', type=str, choices=USER_FILES,
                        default='test_user',
                        metavar='USER',
                        help='user file, e.g.\n' +
                        '{{{}}}'.format(','.join(sample(USER_FILES, 3))))
    parser.add_argument('-k', '--k', type=int, help='for k-means')
    parser.add_argument('-q', '--query', choices=CATEGORIES,
                        metavar='QUERY',
                        help='search for restaurants by category e.g.\n'
                        '{{{}}}'.format(','.join(sample(CATEGORIES, 3))))
    parser.add_argument('-p', '--predict', action='store_true',
                        help='predict ratings for all restaurants')
    parser.add_argument('-r', '--restaurants', action='store_true',
                        help='outputs a list of restaurant names')
    args = parser.parse_args()

    # Output a list of restaurant names
    if args.restaurants:
        print('Restaurant names:')
        for restaurant in sorted(ALL_RESTAURANTS, key=restaurant_name):
            print(repr(restaurant_name(restaurant)))
        exit(0)

    # Select restaurants using a category query
    if args.query:
        restaurants = search(args.query, ALL_RESTAURANTS)
    else:
        restaurants = ALL_RESTAURANTS

    # Load a user
    assert args.user, 'A --user is required to draw a map'
    user = load_user_file('{}.dat'.format(args.user))

    # Collect ratings
    if args.predict:
        ratings = rate_all(user, restaurants, feature_set())
    else:
        restaurants = user_reviewed_restaurants(user, restaurants)
        names = [restaurant_name(r) for r in restaurants]
        ratings = {name: user_rating(user, name) for name in names}

    # Draw the visualization
    if args.k:
        centroids = k_means(restaurants, min(args.k, len(restaurants)))
    else:
        centroids = [restaurant_location(r) for r in restaurants]
    draw_map(centroids, restaurants, ratings)
